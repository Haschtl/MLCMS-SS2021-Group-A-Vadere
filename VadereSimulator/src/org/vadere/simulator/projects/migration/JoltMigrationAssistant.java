package org.vadere.simulator.projects.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.jolttranformation.JoltTransformation;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.LogBufferAppender;
import org.vadere.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static org.vadere.util.io.IOUtils.LEGACY_DIR;
import static org.vadere.util.io.IOUtils.OUTPUT_DIR;
import static org.vadere.util.io.IOUtils.SCENARIO_DIR;


public class JoltMigrationAssistant extends MigrationAssistant {

	private final static Logger logger = Logger.getLogger(JoltMigrationAssistant.class);
	private final LogBufferAppender appender;


	public JoltMigrationAssistant(MigrationOptions options) {
		super(options);
		appender = new LogBufferAppender();
		logger.addAppender(appender);
	}

	public JoltMigrationAssistant() {
		this(MigrationOptions.defaultOptions());
	}

	@Override
	public String getLog() {
		return appender.getMigrationLog();
	}

	@Override
	public void restLog() {
		appender.rest();
	}


	@Override
	public MigrationResult analyzeProject(String projectFolderPath) throws IOException {
		MigrationResult stats = new MigrationResult();

		Path scenarioDir = Paths.get(projectFolderPath, SCENARIO_DIR);
		if (Files.exists(scenarioDir)) {
			stats = analyzeDirectory(scenarioDir, SCENARIO_DIR);
		}

		Path outputDir = Paths.get(projectFolderPath, OUTPUT_DIR);
		if (Files.exists(outputDir)) {
			MigrationResult outputDirStats = analyzeDirectory(outputDir, OUTPUT_DIR);
			stats.add(outputDirStats);
		}
		return stats;
	}

	// will return null if current and target version match
	@Override
	public String convertFile(Path scenarioFilePath, Version targetVersion) throws MigrationException {
		JsonNode node;
		try {
			String json = IOUtils.readTextFile(scenarioFilePath);
			node = StateJsonConverter.deserializeToNode(json);
		} catch (IOException e) {
			logger.error("Error converting File: " + e.getMessage());
			throw new MigrationException("Could not read JsonFile or create Json representation" + e.getMessage());
		}
		restLog();
		logger.info(">> analyzing JSON tree of scenario <" + node.get("name").asText() + ">");
		Version version;
		if (node.get("release") != null) {
			version = Version.fromString(node.get("release").asText());

			if (version == null || version.equalOrSamller(Version.UNDEFINED)) {
				logger.error("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid release create a version transformation and a new idenity transformation");
				throw new MigrationException("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid releasecreate a version transformation and a new idenity transformation");
			}
		} else {
			logger.warn("Version is unknown of scenario <" + node.get("name").asText() + ">! Try to use " + Version.NOT_A_RELEASE.label() + " as Version for transformation.");
			version = Version.NOT_A_RELEASE;
		}

		if (version.equals(targetVersion)) {
			logger.info("Nothing to do current version and target version match");
			restLog();
			return null;
		}

		JsonNode transformedNode = node;
		// apply all transformation from current to latest version.
		for (Version v : Version.listVersionFromTo(version, targetVersion)) {
			logger.info("<" + node.get("name").asText() + "> Start Transform to Version: " + v.label());
			transformedNode = transform(transformedNode, v);
		}

		try {
			restLog();
			return StateJsonConverter.serializeJsonNode(transformedNode);
		} catch (JsonProcessingException e) {
			logger.error("could not serializeJsonNode after Transformation: " + e.getMessage());
			throw new MigrationException("could not serializeJsonNode after Transformation: " + e.getMessage());
		}
	}


	@Override
	public void migrateFile(Path scenarioFilePath, Version targetVersion, Path outputFile) throws MigrationException {
		String json = convertFile(scenarioFilePath, targetVersion);
		if (json == null) {
			logger.info("Nothing todo scenarioFile up-to-date");
			return;
		}

		if (outputFile == null || scenarioFilePath.equals(outputFile)) {
			//overwrite scenarioFile
			Path backupPath = getBackupPath(scenarioFilePath);
			try {
				Files.copy(scenarioFilePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
				IOUtils.writeTextFile(scenarioFilePath.toString(), json);
			} catch (IOException e) {
				logger.error("Cannot overwrite scenarioFile or cannot write new file new version: " + e.getMessage(), e);
				throw new MigrationException("Cannot overwrite scenarioFile or cannot write new file new version: " + e.getMessage(), e);
			}
		} else {
			try {
				IOUtils.writeTextFile(outputFile.toString(), json);
			} catch (IOException e) {
				throw new MigrationException("Cannot write to output file:  " + e.getMessage(), e);
			}
		}
	}


	@Override
	public void revertFile(Path scenarioFile) throws MigrationException {
		Path backupFile = MigrationAssistant.getBackupPath(scenarioFile);
		if (!backupFile.toFile().exists()) {
			logger.error("There does not exist a Backup for the given file");
			logger.error("File: " + scenarioFile.toString());
			logger.error("Backup does not exist: " + backupFile.toString());
		}

		try {
			Files.copy(backupFile, scenarioFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.error("Could not copy legacy backup to current version: " + e.getMessage(), e);
			throw new MigrationException("Could not copy legacy backup to current version: " + e.getMessage(), e);
		}
		try {
			Files.deleteIfExists(backupFile);
		} catch (IOException e) {
			logger.error("Cold not delete old legacy file after reverting File: " + e.getMessage(), e);
		}
	}


	private MigrationResult analyzeDirectory(Path dir, String dirName) throws IOException {

		Path legacyDir = dir.getParent().resolve(LEGACY_DIR).resolve(dirName);

		File[] scenarioFiles = dirName.equals(SCENARIO_DIR) ? IOUtils.getFilesInScenarioDirectory(dir) : IOUtils.getScenarioFilesInOutputDirectory(dir);
		MigrationResult stats = new MigrationResult(scenarioFiles.length);

		for (File file : scenarioFiles) {

			if (dirName.equals(OUTPUT_DIR)) {
				String fileFolder = Paths.get(file.getParent()).getFileName().toString(); // the folder in which the .scenario and the .trajectories file lies
				legacyDir = dir.getParent().resolve(LEGACY_DIR).resolve(OUTPUT_DIR).resolve(fileFolder);
			}

			Path scenarioFilePath = Paths.get(file.getAbsolutePath());
			try {
				if (analyzeScenario(scenarioFilePath, legacyDir, dirName)) {
					stats.legacy++;
				} else {
					stats.upToDate++;
				}
			} catch (MigrationException e) {
				moveFileAddExtension(scenarioFilePath, legacyDir, migrationOptions.getNonmigratabelExtension(), !dirName.equals(SCENARIO_DIR));
				logger.error("!> Can't migrate the scenario to latest version, removed it from the directory (" +
						e.getMessage() + ") If you can fix this problem manually, do so and then remove ." +
						migrationOptions.getNonmigratabelExtension() + " from the file in the " + LEGACY_DIR + "-directory "
						+ "and move it back into the scenarios-directory, it will be checked again when the GUI restarts.");
				stats.notmigratable++;
			}
		}

		if (stats.legacy + stats.notmigratable > 0)
			IOUtils.writeTextFile(legacyDir.resolve("_LOG-" + getTimestamp() + ".txt").toString(), getLog());

		// clean appender for next run with same JoltMigrationAssistant instance
		restLog();
		return stats;
	}

	public JsonNode transform(JsonNode currentJson, Version targetVersion) throws MigrationException {
		JoltTransformation t = JoltTransformation.get(targetVersion.previousVersion());
		return t.applyTransformation(currentJson);
	}

	private boolean analyzeScenario(Path scenarioFilePath, Path legacyDir, String dirName)
			throws IOException, MigrationException {
		String json = IOUtils.readTextFile(scenarioFilePath);
		JsonNode node = StateJsonConverter.deserializeToNode(json);

		String parentPath = dirName.equals(SCENARIO_DIR) ? SCENARIO_DIR + "/" : OUTPUT_DIR + "/" + scenarioFilePath.getParent().getFileName().toString() + "/";

		logger.info(">> analyzing JSON tree of scenario <" + parentPath + node.get("name").asText() + ">");

		Version version;

		if (node.get("release") != null) {
			version = Version.fromString(node.get("release").asText());

			if (version == null || version.equalOrSamller(Version.UNDEFINED)) {
				logger.error("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid release create a version transformation and a new idenity transformation");
				throw new MigrationException("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid releasecreate a version transformation and a new idenity transformation");
			}

			// if enforced migration should be done from baseVersion to latestVersion
			if (migrationOptions.isReapplyLatestMigrationFlag() && migrationOptions.getBaseVersion() != null) {
				version = migrationOptions.getBaseVersion();

			} else if (migrationOptions.isReapplyLatestMigrationFlag()) { // if enforced migration should be done from prev version to latest
				Optional<Version> optVersion = Version.getPrevious(version);
				if (optVersion.isPresent()) {
					version = optVersion.get();
				} else {
					return false;
				}
			} // if no enforced migration should be done and we are at the latest version, no migration is required.
			else if (version == Version.latest()) {
				return false;
			}
		} else {
			logger.warn("Version is unknown of scenario <" + parentPath + node.get("name").asText() + ">! Try to use " + Version.NOT_A_RELEASE.label() + " as Version for transformation.");
			version = Version.NOT_A_RELEASE;
		}

		JsonNode transformedNode = node;
		// apply all transformation from current to latest version.
		for (Version v : Version.listToLatest(version)) {
			logger.info("<" + node.get("name").asText() + "> Start Transform to Version: " + v.label());
			transformedNode = transform(transformedNode, v);
		}
		if (legacyDir != null) {
			logger.info("Scenario Migrated - OK. Move copy of old version to legacllyDir");
			moveFileAddExtension(scenarioFilePath, legacyDir, migrationOptions.getLegacyExtension(), false);
		}
		IOUtils.writeTextFile(scenarioFilePath.toString(), StateJsonConverter.serializeJsonNode(transformedNode));
		return true;
	}

	private void moveFileAddExtension(Path scenarioFilePath, Path legacyDir, String additionalExtension, boolean moveOutputFolder)
			throws IOException {
		Path source = scenarioFilePath;
		Path target = legacyDir.resolve(source.getFileName() + "." + additionalExtension);

		if (moveOutputFolder) {
			source = source.getParent();
			target = Paths.get(legacyDir.toAbsolutePath() + "." + additionalExtension);
		}

		IOUtils.createDirectoryIfNotExisting(target);
		Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); // ensure potential existing files aren't overwritten?
	}
}
