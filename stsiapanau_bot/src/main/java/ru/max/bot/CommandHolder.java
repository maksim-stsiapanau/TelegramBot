package ru.max.bot;

public class CommandHolder {

	private final String command;
	private final boolean rusLang;

	public CommandHolder(String command, boolean rus) {
		this.command = command;
		this.rusLang = rus;
	}

	public String getCommand() {
		return this.command;
	}

	public boolean isRusLang() {
		return this.rusLang;
	}

}
