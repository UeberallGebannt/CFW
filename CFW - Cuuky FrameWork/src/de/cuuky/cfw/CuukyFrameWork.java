package de.cuuky.cfw;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import de.cuuky.cfw.configuration.language.LanguageManager;
import de.cuuky.cfw.configuration.placeholder.MessagePlaceholderManager;
import de.cuuky.cfw.hooking.HookManager;
import de.cuuky.cfw.manager.FrameworkManager;
import de.cuuky.cfw.manager.FrameworkManagerType;
import de.cuuky.cfw.menu.SuperInventoryManager;
import de.cuuky.cfw.serialization.CompatibleLocation;
import de.cuuky.cfw.serialize.CFWSerializeManager;

public class CuukyFrameWork {

	static {
		ConfigurationSerialization.registerClass(CompatibleLocation.class);
	}

	private static final String NAME = "CuukyFrameWork", VERSION = "0.4.4", AUTHOR = "Cuuky";

	/*
	 * CFW - A Bukkit framework
	 * 
	 * CONTACT: { website: "varoplugin.de", discord: 'Cuuky#2783', mail: 'just.cookie.jc@gmail.com' }
	 */

	private JavaPlugin ownerInstance;
	private String consolePrefix;
	private Map<FrameworkManagerType, FrameworkManager> manager;

	public CuukyFrameWork(JavaPlugin pluginInstance) {
		this(pluginInstance, new FrameworkManager[0]);
	}

	public CuukyFrameWork(JavaPlugin pluginInstance, FrameworkManager... manager) {
		this.consolePrefix = "[" + pluginInstance.getName() + "] [CFW] ";

		System.out.println(this.consolePrefix + "Loading " + NAME + " v" + VERSION + " by " + AUTHOR + " for plugin " + pluginInstance.getName() + "...");
		this.ownerInstance = pluginInstance;
		this.manager = new HashMap<>();

		for (FrameworkManager fm : manager) {
			System.out.println(this.consolePrefix + "Added CustomManager " + fm.getClass() + "!");
			this.manager.put(fm.getType(), fm);
		}
	}

	protected FrameworkManager loadManager(FrameworkManagerType type) {
		FrameworkManager manager = this.manager.get(type);
		if (manager == null) {
			try {
				this.manager.put(type, manager = (FrameworkManager) type.getManager().getDeclaredConstructor(JavaPlugin.class).newInstance(this.ownerInstance));
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
				e.printStackTrace();
				throw new IllegalStateException(this.consolePrefix + "Failed to initialize type " + type.toString() + "!");
			}
		}

		return manager;
	}

	public JavaPlugin getPluginInstance() {
		return this.ownerInstance;
	}

	public HookManager getHookManager() {
		return (HookManager) loadManager(FrameworkManagerType.HOOKING);
	}

	public SuperInventoryManager getInventoryManager() {
		return (SuperInventoryManager) loadManager(FrameworkManagerType.INVENTORY);
	}

	public LanguageManager getLanguageManager() {
		return (LanguageManager) loadManager(FrameworkManagerType.LANGUAGE);
	}

	public MessagePlaceholderManager getPlaceholderManager() {
		return (MessagePlaceholderManager) loadManager(FrameworkManagerType.PLACEHOLDER);
	}

	public CFWSerializeManager getSerializeManager() {
		return (CFWSerializeManager) loadManager(FrameworkManagerType.SERIALIZE);
	}
}