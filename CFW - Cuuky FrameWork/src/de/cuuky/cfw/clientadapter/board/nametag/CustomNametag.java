package de.cuuky.cfw.clientadapter.board.nametag;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import de.cuuky.cfw.clientadapter.board.CustomBoard;
import de.cuuky.cfw.clientadapter.board.CustomBoardType;
import de.cuuky.cfw.player.CustomPlayer;
import de.cuuky.cfw.version.BukkitVersion;
import de.cuuky.cfw.version.VersionUtils;

public class CustomNametag<T extends CustomPlayer> extends CustomBoard<T> {

	private static Class<?> visibilityClass;
	private static Method setVisibilityMethod;
	private static Object visibilityNever, visibilityAlways;

	static {
		if (VersionUtils.getVersion().isHigherThan(BukkitVersion.ONE_7)) {
			try {
				visibilityClass = Class.forName("org.bukkit.scoreboard.NameTagVisibility");
				visibilityNever = visibilityClass.getDeclaredField("NEVER").get(null);
				visibilityAlways = visibilityClass.getDeclaredField("ALWAYS").get(null);
				setVisibilityMethod = Team.class.getDeclaredMethod("setNameTagVisibility", visibilityClass);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 0 = name, 1 = prefix, 2 = suffix
	private String[] nametagContent;
	private String oldName;
	private boolean initalized, nametagShown;

	public CustomNametag(T player) {
		super(CustomBoardType.NAMETAG, player);

		this.nametagContent = new String[3];
		this.nametagShown = true;
	}

	@Override
	public void onEnable() {
		Scoreboard sb = player.getPlayer().getScoreboard();
		if (sb.getTeams().size() > 0)
			for (int i = sb.getTeams().size() - 1; i != 0; i--) {
				Team team = (Team) sb.getTeams().toArray()[i];
				try {
					if (!team.getName().startsWith("team-"))
						team.unregister();
				} catch (IllegalStateException e) {}
			}

		giveAll();
		this.initalized = true;
	}

	public void startDelayedRefresh() {
		this.manager.getOwnerInstance().getServer().getScheduler().scheduleSyncDelayedTask(this.manager.getOwnerInstance(), new Runnable() {

			@Override
			public void run() {
				update();
			}
		}, 1);
	}

	private boolean refreshPrefix() {
		String newName = this.player.getUpdateHandler().getNametagName();
		if (newName != null && newName.startsWith("team-"))
			throw new IllegalArgumentException("Player nametag name cannot start with 'team-'");

		String[] check = new String[] { newName, this.player.getUpdateHandler().getNametagPrefix(), this.player.getUpdateHandler().getNametagSuffix() };
		for (int i = 0; i < check.length; i++) {
			String newContent = check[i];
			if (newContent == null)
				continue;

			if (newContent.length() > 16)
				check[i] = newContent.substring(0, 16);
		}

		boolean showNametag = this.player.getUpdateHandler().isNametagVisible();
		boolean changed = !Arrays.equals(this.nametagContent, check) || showNametag != this.nametagShown;
		if (!changed)
			return false;

		this.oldName = nametagContent[0];
		this.nametagContent = check;
		this.nametagShown = showNametag;
		return true;
	}

	private void updateFor(Scoreboard board, CustomNametag<T> nametag) {
		Team oldTeam = board.getTeam(nametag.getOldName() != null ? nametag.getOldName() : this.player.getPlayer().getName());
		if (oldTeam != null)
			oldTeam.unregister();

		String teamName = nametag.getName() == null ? this.player.getPlayer().getName() : nametag.getName();
		Team team = board.getTeam(teamName);
		if (team == null) {
			team = board.registerNewTeam(teamName);
			team.addPlayer(nametag.getPlayer().getPlayer());
		}

		setVisibility(team, nametag.isNametagShown());
		if (nametag.getPrefix() != null) {
			if (team.getPrefix() == null)
				team.setPrefix(nametag.getPrefix());
			else if (!team.getPrefix().equals(nametag.getPrefix()))
				team.setPrefix(nametag.getPrefix());
		} else
			team.setPrefix(null);

		if (nametag.getSuffix() != null) {
			if (team.getSuffix() == null)
				team.setSuffix(nametag.getSuffix());
			else if (!team.getSuffix().equals(nametag.getSuffix()))
				team.setSuffix(nametag.getSuffix());
		} else
			team.setSuffix(null);
	}

	@Override
	protected void onUpdate() {
		if (!refreshPrefix())
			return;

		setToAll();
	}

	public void giveAll() {
		Scoreboard board = player.getPlayer().getScoreboard();
		for (CustomBoard<T> nametag : this.manager.getBoards(CustomBoardType.NAMETAG))
			if (((CustomNametag<T>) nametag).isInitalized())
				updateFor(board, (CustomNametag<T>) nametag);
	}

	public void setToAll() {
		for (Player toSet : VersionUtils.getOnlinePlayer())
			updateFor(toSet.getScoreboard(), this);
	}

	public String getPrefix() {
		return this.nametagContent[1];
	}

	public String getName() {
		return this.nametagContent[0];
	}

	public String getSuffix() {
		return this.nametagContent[2];
	}

	public String getOldName() {
		return oldName;
	}

	public boolean isNametagShown() {
		return nametagShown;
	}

	public boolean isInitalized() {
		return initalized;
	}

	private static void setVisibility(Team team, boolean shown) {
		if (!VersionUtils.getVersion().isHigherThan(BukkitVersion.ONE_7))
			return;

		try {
			setVisibilityMethod.invoke(team, shown ? visibilityAlways : visibilityNever);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}