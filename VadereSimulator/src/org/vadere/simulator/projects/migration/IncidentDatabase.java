package org.vadere.simulator.projects.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.migration.MigrationAssistant.Version;
import org.vadere.simulator.projects.migration.incidents.AddTextNodeIncident;
import org.vadere.simulator.projects.migration.incidents.DeletionIncident;
import org.vadere.simulator.projects.migration.incidents.Incident;
import org.vadere.simulator.projects.migration.incidents.MissingMainModelIncident;
import org.vadere.simulator.projects.migration.incidents.RelocationIncident;
import org.vadere.simulator.projects.migration.incidents.RenameInArrayIncident;
import org.vadere.simulator.projects.migration.incidents.RenameIncident;
import org.vadere.simulator.projects.migration.incidents.specialized.AttributesPotentialCompactVSosmIncident;
import org.vadere.simulator.projects.migration.incidents.specialized.MoveSpawnDelayIntoDistributionParametersIncident;

import static org.vadere.simulator.projects.migration.MigrationAssistant.Version.*;

public class IncidentDatabase {

	private Map<Version, List<Incident>> incidents = new LinkedHashMap<>();

	private IncidentDatabase() {

		/*
		A list of incidents always marks the possible incidents from one version to another.
		The key in the Map represents the departure-version. An incident that's added to the
		Version.NOT_A_RELEASE list for instance, get's checked for applicability when making
		the migration <from> Version.NOT_A_RELEASE <to> Version.V0_1
		 */

		// - - - - - - - - - - - - "not a release" to "0.1" - - - - - - - - - - - -

		incidents.put(Version.NOT_A_RELEASE, new LinkedList<>());

		addIncident(NOT_A_RELEASE, new RelocationIncident(
				"finishTime",
				path("vadere", "topography", "attributes"),
				path("vadere", "attributesSimulation")));

		addIncident(NOT_A_RELEASE, new RelocationIncident(
				"attributesPedestrian",
				path("vadere"),
				path("vadere", "topography")));

		addIncident(NOT_A_RELEASE, new DeletionIncident(
				path("vadere", "topography", "pedestrians")));

		addIncident(NOT_A_RELEASE, new RenameInArrayIncident(
				path("vadere", "topography", "dynamicElements"),
				"nextTargetListPosition",
				"nextTargetListIndex"));

		for (String oldName : LookupTables.version0to1_ModelRenaming.keySet()) {
			String newName = LookupTables.version0to1_ModelRenaming.get(oldName);
			addIncident(NOT_A_RELEASE, new RenameIncident(
					path("vadere", "attributesModel", oldName), newName));
		}

		addIncident(NOT_A_RELEASE, new MissingMainModelIncident( // must come AFTER the model renaming that was done in the loop before
				path("vadere"),
				JsonConverter.MAIN_MODEL_KEY,
				path("vadere", "attributesModel")));

		addIncident(NOT_A_RELEASE, new AddTextNodeIncident(
				path(),
				"description", ""));

		addIncident(NOT_A_RELEASE, new AttributesPotentialCompactVSosmIncident()); // requested by Bene
		addIncident(NOT_A_RELEASE, new MoveSpawnDelayIntoDistributionParametersIncident()); // requested by Jakob

		incidents.get(Version.NOT_A_RELEASE).add(new DeletionIncident(path("topographyhash")));
		incidents.get(Version.NOT_A_RELEASE).add(new DeletionIncident(path("attributeshash")));

		// - - - - - - - - - - - - "0.1" to "0.2" - - - - - - - - - - - -

		incidents.put(V0_1, new LinkedList<>());
		//addIncident(V0_1, ...

		// - - - - - - - - - - - - "0.?" to "?" - - - - - - - - - - - -

		//incidents.put(V0_?, new LinkedList<>());
		//addIncident(V0_, ...
	}

	private void addIncident(Version version, Incident incident) {
		incidents.get(version).add(incident);
	}

	public List<Incident> getPossibleIncidentsFor(Version version) {
		if (incidents.containsKey(version)) {
			return incidents.get(version);
		}
		return new ArrayList<>();
	}

	public static List<String> path(String... entries) {
		return Arrays.asList(entries);
	}

	private static IncidentDatabase instance = null;

	public static IncidentDatabase getInstance() {
		if (instance == null) {
			instance = new IncidentDatabase();
		}
		return instance;
	}

}


