package ru.max.bot;

/**
 * Hold command and russian language flag
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
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
