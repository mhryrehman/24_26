package ca.uhn.fhir.jpa.starter.curemd.sched;

import ca.uhn.fhir.batch2.coordinator.JobDefinitionRegistry;
import ca.uhn.fhir.batch2.model.JobDefinition;
import ca.uhn.fhir.batch2.model.JobInstance;
import ca.uhn.fhir.model.api.IModelJson;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DisabledJobDefinitionRegistry extends JobDefinitionRegistry {

	@Override
	public synchronized <PT extends IModelJson> void addJobDefinition(JobDefinition<PT> theDefinition) {
		// No-op
	}

	@Override
	public synchronized <PT extends IModelJson> boolean addJobDefinitionIfNotRegistered(JobDefinition<PT> theDefinition) {
		return false;
	}

	@Override
	public Optional<JobDefinition<?>> getLatestJobDefinition(String theJobDefinitionId) {
		return Optional.empty();
	}

	@Override
	public Optional<JobDefinition<?>> getJobDefinition(String theJobDefinitionId, int theJobDefinitionVersion) {
		return Optional.empty();
	}

	@Override
	public JobDefinition<?> getJobDefinitionOrThrowException(String theJobDefinitionId, int theJobDefinitionVersion) {
		throw new UnsupportedOperationException("Bulk export jobs are disabled on this server.");
	}

	@Override
	public <T extends IModelJson> JobDefinition<T> getJobDefinitionOrThrowException(JobInstance theJobInstance) {
		throw new UnsupportedOperationException("Bulk export jobs are disabled on this server.");
	}

	@Override
	public void setJobDefinition(JobInstance theInstance) {
		throw new UnsupportedOperationException("Bulk export jobs are disabled on this server.");
	}

	@Override
	public List<String> getJobDefinitionIds() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Integer> getJobDefinitionVersions(String theDefinitionId) {
		return Collections.emptyList();
	}

	@Override
	public boolean isEmpty() {
		return true;
	}
}
