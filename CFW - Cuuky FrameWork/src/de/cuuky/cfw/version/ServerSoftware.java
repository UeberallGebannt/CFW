package de.cuuky.cfw.version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ServerSoftware {

	BUKKIT("Bukkit", false, "Bukkit"),
	SPIGOT("Spigot", false, "Spigot"),
	PAPER("PaperSpigot", false, "PaperSpigot", "Paper"),
	TACO("TacoSpigot", false, "TacoSpigot"),
	MAGMA("Magma", true, "Magma"),
	CAULDRON("Cauldron", true, "Cauldron"),
	THERMOS("Thermos", true, "Thermos"),
	URANIUM("Uranium", true, "Uranium"),
	UNKNOWN("Unknown", true);

	private static List<Character> abc;

	static {
		abc = new ArrayList<Character>(Arrays.asList(new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' }));
	}

	private String name;
	private String[] versionnames;
	private boolean modsupport;

	private ServerSoftware(String name, boolean modsupport, String... versionnames) {
		this.name = name;
		this.versionnames = versionnames;
		this.modsupport = modsupport;
	}

	public String getName() {
		return this.name;
	}

	public String[] getVersionNames() {
		return this.versionnames;
	}

	public boolean hasModSupport() {
		return this.modsupport;
	}

	public static ServerSoftware getServerSoftware(String version, String name) {
		version = version.toLowerCase();
		name = name.toLowerCase();

		ServerSoftware found = null;
		String foundName = null;
		for (ServerSoftware software : values()) {
			for (String softwareName : software.getVersionNames()) {
				softwareName = softwareName.toLowerCase();

				if (!version.contains(softwareName) || found != null && softwareName.length() < foundName.length())
					continue;

				found = software;
				foundName = softwareName;
			}
		}

		if (found == null)
			found = UNKNOWN;
		else if (found != UNKNOWN) {
			int location = version.indexOf(foundName);

			if (location - 1 > -1)
				if (abc.contains(version.charAt(location - 1)))
					found = UNKNOWN;

			if (location + foundName.length() + 1 < version.length())
				if (abc.contains(version.charAt(location + foundName.length())))
					found = UNKNOWN;
		}

		if (found == UNKNOWN) {
			for (ServerSoftware software : values()) {
				for (String softwareName : software.getVersionNames()) {
					softwareName = softwareName.toLowerCase();

					if (!name.equals(softwareName))
						continue;

					found = software;
					foundName = softwareName;
				}
			}
		}

		return found;
	}
}