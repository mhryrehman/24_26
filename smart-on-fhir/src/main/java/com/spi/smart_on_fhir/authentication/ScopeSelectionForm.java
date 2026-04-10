package com.spi.smart_on_fhir.authentication;

import com.spi.smart_on_fhir.factory.ScopeSelectionFormFactory;
import com.spi.smart_on_fhir.utils.Constants;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Author: Yasir Rehman
 * Description:
 * Present a scope selection picker for requested client scopes and the
 * user can select multiple scopes. The selection is stored in a UserSessionNote
 * with name ""selected_scopes.
 */
public class ScopeSelectionForm implements Authenticator {

    private static final Logger LOG = Logger.getLogger(ScopeSelectionForm.class);
    private static final Pattern CATEGORY_SCOPE_PATTERN = Pattern.compile("^(?:.*/)?(Condition|Observation)\\.rs\\?category=(.+)$");


    /**
     * Authenticates the user by presenting a scope selection form if non-default
     * requested scopes are found. Otherwise, skips the scope selection.
     *
     * @param context the authentication flow context
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        LOG.info("ScopeSelectionForm's authenticate method called.");

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        ClientModel client = authSession.getClient();

        // Retrieve requested scopes from the authentication session
        String requestedScopesString = authSession.getClientNote(OIDCLoginProtocol.SCOPE_PARAM);
        if (requestedScopesString == null || requestedScopesString.isEmpty()) {
            LOG.info("No requested scopes found, skipping scope selection.");
            context.success();
            return;
        }

        // Parse requested scopes
        Set<String> requestedScopes = Arrays.stream(requestedScopesString.split("\\s+"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LOG.info("Requested scopes: " + requestedScopes);
        if (requestedScopes.isEmpty()) {
            LOG.info("Requested scopes list is empty, skipping scope selection.");
            context.success();
            return;
        }

        // Retrieve default client scopes
        List<String> defaultScopes = client.getClientScopes(true).values().stream()
                .map(ClientScopeModel::getName)
                .collect(Collectors.toList());

        LOG.info("Default client scopes: " + defaultScopes);
        authSession.setUserSessionNote(Constants.DEFAULT_SCOPES_NOTE, String.join(" ", defaultScopes));

        // Get optional client scopes once (name -> ClientScopeModel)
        Map<String, ClientScopeModel> optionalScopesMap = client.getClientScopes(false);
        LOG.info("Optional client scopes: " + optionalScopesMap.keySet());

        if(isGranularEnabled(context)){
            boolean conditionRequested = requestedScopes.contains(Constants.PATIENT_CONDITION_RS);
            boolean observationRequested = requestedScopes.contains(Constants.PATIENT_OBSERVATION_RS);
            if(conditionRequested){
                Set<String> granularConditionScopes = findGranularCategoryScopes(Constants.PATIENT_CONDITION_RS, optionalScopesMap);
                LOG.info("Granular Condition scopes found: " + granularConditionScopes);
//                requestedScopes.remove(Constants.PATIENT_CONDITION_RS);
                requestedScopes.addAll(granularConditionScopes);
            }
            if(observationRequested){
                Set<String> granularObservationScopes = findGranularCategoryScopes(Constants.PATIENT_OBSERVATION_RS, optionalScopesMap);
                LOG.info("Granular Observation scopes found: " + granularObservationScopes);
//                requestedScopes.remove(Constants.PATIENT_OBSERVATION_RS);
                requestedScopes.addAll(granularObservationScopes);
            }
        }

        // Build map of requested optional scopes: key = scope name, value = consent text (or name)
        Map<String, String> reqOptScopesMap = requestedScopes.stream()
                .filter(optionalScopesMap::containsKey) // only those that are actually optional
                .collect(Collectors.toMap(
                        scopeName -> scopeName,
                        scopeName -> {
                            ClientScopeModel scope = optionalScopesMap.get(scopeName);
                            String text = scope.getConsentScreenText();
                            return (text == null || text.isBlank()) ? scope.getName() : text;
                        }
                ));

        LOG.info("Requested optional scopes to display: " + reqOptScopesMap);

        if (reqOptScopesMap.isEmpty()) {
            LOG.info("No requested optional scopes to display, skipping scope selection.");
            context.success();
            return;
        }

        // Pass scopes to the UI
        Response response = context.form()
                .setAttribute("scopesMap", reqOptScopesMap)
                .createForm("scope-select-form.ftl");
        context.challenge(response);
    }

    /**
     * Filters out default scopes from the requested scopes list.
     *
     * @param requestedScopes the list of requested scopes
     * @param defaultScopes   the list of default scopes
     * @return a list of non-default scopes
     */
    private List<String> filterNonDefaultScopes(List<String> requestedScopes, List<String> defaultScopes) {
        if (defaultScopes == null || defaultScopes.isEmpty()) {
            return requestedScopes;
        }
        return requestedScopes.stream()
                .filter(scope -> !defaultScopes.contains(scope)) // Exclude scopes that are in defaultScopes
                .collect(Collectors.toList());
    }

    /**
     * Handles the user's scope selection and stores the selected scopes in session notes.
     *
     * @param context the authentication flow context
     */
    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<String> selectedScopes = formData.get("selectedScopes");

        if (selectedScopes == null || selectedScopes.isEmpty()) {
            LOG.warn("No scopes selected. Cancelling login.");
            context.cancelLogin();
            return;
        }

        LOG.info("User selected scopes: " + selectedScopes);

        // Store selected scopes in session notes
        context.getAuthenticationSession().setUserSessionNote(Constants.SELECTED_SCOPES_NOTE, String.join(" ", selectedScopes));

        String defaultScopes = context.getAuthenticationSession().getAuthNote(Constants.DEFAULT_SCOPES_NOTE);
        LOG.info("Selected scopes: " + selectedScopes);
        LOG.info("Default scopes: " + defaultScopes);
        //if selectedScopes is null or empty then no need to add default scopes.
        if (!selectedScopes.isEmpty()) {
            // Initialize scope lists
            List<String> defaultScopeList = parseScopes(defaultScopes);
            selectedScopes.addAll(defaultScopeList);
        }
        String finalScopes = String.join(" ", selectedScopes);
        LOG.info("Final scope set in note scope : " + finalScopes);
// 1) set auth & user session notes (what you already do)
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        // 1) Primary: set client note for OIDC scope param so TokenManager / ClientSessionContext picks it up
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, finalScopes);

        // 2) Helpful: also set auth note and user session note
        authSession.setAuthNote(OIDCLoginProtocol.SCOPE_PARAM, finalScopes);
        authSession.setUserSessionNote(Constants.SELECTED_SCOPES_NOTE, finalScopes);

        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions
    }

    @Override
    public void close() {
    }

    private boolean isGranularEnabled(AuthenticationFlowContext context) {

        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null) {
            return false; // no config attached to execution
        }

        Map<String, String> config = configModel.getConfig();
        if (config == null) {
            return false;
        }

        return Boolean.parseBoolean(
                config.getOrDefault(
                        ScopeSelectionFormFactory.CFG_ENABLE_GRANULAR,
                        "false"
                )
        );
    }

    public static Set<String> findGranularCategoryScopes(
            String requestedScope,
            Map<String, ClientScopeModel> optionalScopesMap) {

        if (requestedScope == null || optionalScopesMap == null) {
            return Set.of();
        }

        return optionalScopesMap.keySet().stream()
                .filter(scope -> scope.startsWith(requestedScope))
                .filter(CATEGORY_SCOPE_PATTERN.asPredicate())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }


    /**
     * Parses a whitespace-separated scope string into a list of scopes.
     *
     * @param scopes A string of scopes separated by whitespace.
     * @return A list of individual scopes or an empty list if the input is null or empty.
     */
    private List<String> parseScopes(String scopes) {
        if (scopes == null || scopes.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(scopes.split(" "))
                .map(String::trim)
                .filter(scope -> !scope.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
