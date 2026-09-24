package org.mockbukkit.mockbukkit.plugin;

import com.destroystokyo.paper.utils.PaperPluginLogger;
import com.google.common.base.Preconditions;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.exception.PluginClassNotFoundException;
import org.mockbukkit.mockbukkit.exception.UnimplementedOperationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/*
 * Изменено для песочницы Byte (исходный код MockBukkit распространяется по лицензии MIT):
 * - класс final: подкласс загрузчика мог бы определять классы с любыми правами;
 * - родитель — загрузчик самого MockBukkit, а не системный: в песочнице плагин ученика и библиотеки
 *   загружаются отдельным загрузчиком;
 * - прокси плагина определяется напрямую через defineClass: ByteBuddy INJECTION требует sun.misc.Unsafe,
 *   которого в песочнице нет.
 */
public final class MockBukkitConfiguredPluginClassLoader extends URLClassLoader implements ConfiguredPluginClassLoader
{

	private final ServerMock server;
	private final PluginDescriptionFile description;
	private final File dataFolder;
	private final File pluginFile;
	private JarFile jarFile = null;
	private final PluginClassLoaderGroup classLoaderGroup = new MockBukkitPluginClassLoaderGroup();

	public MockBukkitConfiguredPluginClassLoader(
			ServerMock server,
			PluginDescriptionFile description,
			File dataFolder,
			File pluginFile
	)
	{
		super(new URL[0], MockBukkitConfiguredPluginClassLoader.class.getClassLoader());
		this.server = server;
		this.description = description;
		this.dataFolder = dataFolder;
		this.pluginFile = pluginFile;
	}

	public void setJarFile(JarFile jarFile)
	{
		this.jarFile = jarFile;
	}

	@Override
	public PluginMeta getConfiguration()
	{
		return description;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
	{
		Class<?> groupLoadedClass = classLoaderGroup.getClassByName(name, resolve, this);
		if (groupLoadedClass == null)
		{
			return super.loadClass(name, resolve);
		}
		else
		{
			return groupLoadedClass;
		}
	}

	@Override
	public Class<?> loadClass(@NotNull String name, boolean resolve, boolean checkGlobal, boolean checkLibraries) throws ClassNotFoundException
	{
		return loadClass(name, resolve);
	}

	@Override
	protected Class<?> findClass(String name)
	{
		try
		{
			Preconditions.checkNotNull(jarFile, "No jar file selected");
			ZipEntry entry = jarFile.getEntry(name.replace('.', '/') + ".class");
			InputStream inputStream = jarFile.getInputStream(entry);
			byte[] array = inputStream.readAllBytes();
			return defineClass(name, array, 0, array.length);
		}
		catch (IOException e)
		{
			throw new PluginClassNotFoundException(e);
		}
	}

	public Class<? extends Plugin> loadProxyClass(Class<? extends Plugin> target)
	{
		String name = target.getSimpleName() + "Proxy";
		byte[] bytes = new ByteBuddy()
				.subclass(target, ConstructorStrategy.Default.IMITATE_SUPER_CLASS)
				.name(name)
				.make()
				.getBytes();
		return defineClass(name, bytes, 0, bytes.length).asSubclass(Plugin.class);
	}

	@Override
	public void init(JavaPlugin plugin)
	{
		plugin.init(server, description, dataFolder, pluginFile, this, getConfiguration(), PaperPluginLogger.getLogger(getConfiguration()));
	}

	@Override
	public @Nullable JavaPlugin getPlugin()
	{
		// TODO Auto-generated method stub
		throw new UnimplementedOperationException();
	}

	@Override
	public @Nullable PluginClassLoaderGroup getGroup()
	{
		return classLoaderGroup;
	}

	@Override
	public void close()
	{
		// TODO Auto-generated method stub
		throw new UnimplementedOperationException();
	}

}
