<#import "template.ftl" as layout>
<#import "components/atoms/button.ftl" as button>
<#import "components/atoms/button-group.ftl" as buttonGroup>

<img class="logo" src="https://www.curemd.com/cmd/cmd-assets/images/curemd-vector.svg" alt="CureMD Logo" />

<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        Patient selection
    <#elseif section = "header">
        Patient selection
    <#elseif section = "form">
        <form id="patient-selection" action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-u2f-login-form" method="post">
            <p>Select the person whose data you want to access.</p>
            <div class="patient-selection-list">
                <#list patients as patient>
                    <div class="patient-selection">
                    <div>
						<input type="radio" name="patient" id="${patient.id}" value="${patient.id}"/>
						<label for="${patient.id}">${patient.name}</label>
					</div>
					<span class="patient-date">${patient.dob}</span>
                    </div>
                </#list>
            </div>
				   
				   <@buttonGroup.kw>
				  <@button.kw color="primary" name="login" type="submit">
					Submit
				  </@button.kw>
				</@buttonGroup.kw>

        </form>
    </#if>
</@layout.registrationLayout>