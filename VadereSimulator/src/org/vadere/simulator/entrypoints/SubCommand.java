package org.vadere.simulator.entrypoints;

public enum SubCommand {

	PROJECT_RUN("project-run"),
	SCENARO_RUN("scenario-run"),
	SUQ("suq"),
	MIGRATE("migrate");

	private String cmdName;

	SubCommand(String s) {
		this.cmdName = s;
	}

	public String getCmdName(){
		return this.cmdName;
	}
}
