/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.BootstrapRegistry.Scope;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributors.BinderOption;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * Wrapper around a {@link ConfigurableEnvironment} that can be used to import and apply
 * {@link ConfigData}. Configures the initial set of
 * {@link ConfigDataEnvironmentContributors} by wrapping property sources from the Spring
 * {@link Environment} and adding the initial set of locations.
 * <p>
 * The initial locations can be influenced through the {@link #LOCATION_PROPERTY},
 * {@value #ADDITIONAL_LOCATION_PROPERTY} and {@value #IMPORT_PROPERTY} properties. If no
 * explicit properties are set, the {@link #DEFAULT_SEARCH_LOCATIONS} will be used.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 *
 * @apiNote 该类是 springboot 配置数据加载和管理的核心组件，负责从多个源加载、解析和处理配置数据，
 * 并将这些配置数据添加到当前应用的 Environment 中。
 */
class ConfigDataEnvironment {

	/**
	 * Property used override the imported locations.
	 */
	static final String LOCATION_PROPERTY = "spring.config.location";

	/**
	 * Property used to provide additional locations to import.
	 */
	static final String ADDITIONAL_LOCATION_PROPERTY = "spring.config.additional-location";

	/**
	 * Property used to provide additional locations to import.
	 */
	static final String IMPORT_PROPERTY = "spring.config.import";

	/**
	 * Property used to determine what action to take when a
	 * {@code ConfigDataNotFoundAction} is thrown.
	 * @see ConfigDataNotFoundAction
	 */
	static final String ON_NOT_FOUND_PROPERTY = "spring.config.on-not-found";

	/**
	 * Default search locations used if not {@link #LOCATION_PROPERTY} is found.
	 */
	static final ConfigDataLocation[] DEFAULT_SEARCH_LOCATIONS;
	static {
		List<ConfigDataLocation> locations = new ArrayList<>();
		locations.add(ConfigDataLocation.of("optional:classpath:/;optional:classpath:/config/"));
		locations.add(ConfigDataLocation.of("optional:file:./;optional:file:./config/;optional:file:./config/*/"));
		DEFAULT_SEARCH_LOCATIONS = locations.toArray(new ConfigDataLocation[0]);
	}

	private static final ConfigDataLocation[] EMPTY_LOCATIONS = new ConfigDataLocation[0];

	private static final Bindable<ConfigDataLocation[]> CONFIG_DATA_LOCATION_ARRAY = Bindable
		.of(ConfigDataLocation[].class);

	private static final Bindable<List<String>> STRING_LIST = Bindable.listOf(String.class);

	private static final BinderOption[] ALLOW_INACTIVE_BINDING = {};

	private static final BinderOption[] DENY_INACTIVE_BINDING = { BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE };

	private final DeferredLogFactory logFactory;

	private final Log logger;

	private final ConfigDataNotFoundAction notFoundAction;

	private final ConfigurableBootstrapContext bootstrapContext;

	private final ConfigurableEnvironment environment;

	private final ConfigDataLocationResolvers resolvers;

	private final Collection<String> additionalProfiles;

	private final ConfigDataEnvironmentUpdateListener environmentUpdateListener;

	private final ConfigDataLoaders loaders;

	private final ConfigDataEnvironmentContributors contributors;

	/**
	 * Create a new {@link ConfigDataEnvironment} instance.
	 * @param logFactory the deferred log factory
	 * @param bootstrapContext the bootstrap context
	 * @param environment the Spring {@link Environment}.
	 * @param resourceLoader {@link ResourceLoader} to load resource locations
	 * @param additionalProfiles any additional profiles to activate
	 * @param environmentUpdateListener optional
	 * {@link ConfigDataEnvironmentUpdateListener} that can be used to track
	 * {@link Environment} updates.
	 */
	ConfigDataEnvironment(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			ConfigurableEnvironment environment, ResourceLoader resourceLoader, Collection<String> additionalProfiles,
			ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
		Binder binder = Binder.get(environment);
		UseLegacyConfigProcessingException.throwIfRequested(binder);
		this.logFactory = logFactory;
		this.logger = logFactory.getLog(getClass());
		// 从属性 spring.config.on-not-found 中获取文件找不到的执行逻辑
		this.notFoundAction = binder.bind(ON_NOT_FOUND_PROPERTY, ConfigDataNotFoundAction.class)
			.orElse(ConfigDataNotFoundAction.FAIL);
		this.bootstrapContext = bootstrapContext;
		this.environment = environment;
		// 从 spring.factories 中获取 ConfigDataLocationResolver 实现。(可以自己实现，扩展点之一)
		// 同时这里面会传入 boostrapContext/resourceLoader/Binder 等参数用于构造参数反射
		this.resolvers = createConfigDataLocationResolvers(logFactory, bootstrapContext, binder, resourceLoader);
		this.additionalProfiles = additionalProfiles;
		this.environmentUpdateListener = (environmentUpdateListener != null) ? environmentUpdateListener
				: ConfigDataEnvironmentUpdateListener.NONE;
		// 初始化 loaders，从 spring.factories 中获取所有的 ConfigDataLoader 并用反射进行实例化
		this.loaders = new ConfigDataLoaders(logFactory, bootstrapContext, resourceLoader.getClassLoader());
		// 将环境中已有的属性源以及需要导入配置的位置封装为 contributors
		this.contributors = createContributors(binder);
	}

	protected ConfigDataLocationResolvers createConfigDataLocationResolvers(DeferredLogFactory logFactory,
			ConfigurableBootstrapContext bootstrapContext, Binder binder, ResourceLoader resourceLoader) {
		return new ConfigDataLocationResolvers(logFactory, bootstrapContext, binder, resourceLoader);
	}

	private ConfigDataEnvironmentContributors createContributors(Binder binder) {
		this.logger.trace("Building config data environment contributors");
		MutablePropertySources propertySources = this.environment.getPropertySources();
		List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>(propertySources.size() + 10);
		PropertySource<?> defaultPropertySource = null;
		// 遍历当前环境中的所有 propertySource，并为每个 propertySource 创建一个 ConfigDataEnvironmentContributor
		for (PropertySource<?> propertySource : propertySources) {
			if (DefaultPropertiesPropertySource.hasMatchingName(propertySource)) {
				defaultPropertySource = propertySource;
			}
			else {
				this.logger.trace(LogMessage.format("Creating wrapped config data contributor for '%s'",
						propertySource.getName()));
				// 将环境中已有的属性源封装为 EXISTING 类型的 contributor
				contributors.add(ConfigDataEnvironmentContributor.ofExisting(propertySource));
			}
		}

		// spring.config.import/additional-location/location 这些属性指定的配置文件也需要创建 contributor
		// 封装为 INITIAL_IMPORT 类型的 contributor
		contributors.addAll(getInitialImportContributors(binder));
		if (defaultPropertySource != null) {
			this.logger.trace("Creating wrapped config data contributor for default property source");
			contributors.add(ConfigDataEnvironmentContributor.ofExisting(defaultPropertySource));
		}
		return createContributors(contributors);
	}

	protected ConfigDataEnvironmentContributors createContributors(
			List<ConfigDataEnvironmentContributor> contributors) {
		return new ConfigDataEnvironmentContributors(this.logFactory, this.bootstrapContext, contributors);
	}

	ConfigDataEnvironmentContributors getContributors() {
		return this.contributors;
	}

	private List<ConfigDataEnvironmentContributor> getInitialImportContributors(Binder binder) {
		List<ConfigDataEnvironmentContributor> initialContributors = new ArrayList<>();
		// spring.config.import 指定的路径封装成 contributor
		addInitialImportContributors(initialContributors, bindLocations(binder, IMPORT_PROPERTY, EMPTY_LOCATIONS));
		// spring.config.additional-location 指定的路径封装成 contributor
		addInitialImportContributors(initialContributors,
				bindLocations(binder, ADDITIONAL_LOCATION_PROPERTY, EMPTY_LOCATIONS));
		// spring.config.location 如果没有配置，就是用默认路径，即 classpath:/，classpath:/config，file:./ 等等这些
		// 这里也证明了如果配置了 spring.config.location，就不会再去扫描默认路径下的配置文件
		// todo: 即使配置了 spring.config.location，在这里也不起作用，还是去加载默认路径，why？是代码中配置没起作用？
		addInitialImportContributors(initialContributors,
				bindLocations(binder, LOCATION_PROPERTY, DEFAULT_SEARCH_LOCATIONS));
		return initialContributors;
	}

	private ConfigDataLocation[] bindLocations(Binder binder, String propertyName, ConfigDataLocation[] other) {
		return binder.bind(propertyName, CONFIG_DATA_LOCATION_ARRAY).orElse(other);
	}

	private void addInitialImportContributors(List<ConfigDataEnvironmentContributor> initialContributors,
			ConfigDataLocation[] locations) {
		for (int i = locations.length - 1; i >= 0; i--) {
			initialContributors.add(createInitialImportContributor(locations[i]));
		}
	}

	private ConfigDataEnvironmentContributor createInitialImportContributor(ConfigDataLocation location) {
		this.logger.trace(LogMessage.format("Adding initial config data import from location '%s'", location));
		return ConfigDataEnvironmentContributor.ofInitialImport(location);
	}

	/**
	 * Process all contributions and apply any newly imported property sources to the
	 * {@link Environment}.
	 */
	void processAndApply() {
		// 创建 ConfigDataImporter 对象，该对象用于加载配置数据，并将配置数据保存到 Contributors 中
		ConfigDataImporter importer = new ConfigDataImporter(this.logFactory, this.notFoundAction, this.resolvers,
				this.loaders);
		registerBootstrapBinder(this.contributors, null, DENY_INACTIVE_BINDING);
		// 处理类型为 INITIAL_IMPORT 的 contributor，到这里为 INITIAL_IMPORT 只的就是 spring.config.location 对应的资源，
		// 如果没配置的话就是默认路径对应的资源，并保存到 contributor 的 children 中，并标记为 BOUND_IMPORT。
		// 解析并加载路径下的配置文件，拿到所有可能得配置文件（application.properties或application.yml等等）
		ConfigDataEnvironmentContributors contributors = processInitial(this.contributors, importer);
		// 创建 profile 上下文，用来保存当前项目激活的 profile
		ConfigDataActivationContext activationContext = createActivationContext(
				contributors.getBinder(null, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE));
		// 在不考虑 profile 的情况下，将 processInitial 那步未处理的 contributor 进行处理，同样保存到 contributor 的 children 下，并标记为 BOUND_IMPORT
		contributors = processWithoutProfiles(contributors, importer, activationContext);
		// 确定当前项目激活的 profile，并保存到 profile 上下文中 -> activationContext
		activationContext = withProfiles(contributors, activationContext);
		// 根据已确定的 profile，加载配置文件中的激活的 profile，保存到当前 contributor 的 children 中，并标记为 BOUND_IMPORT
		contributors = processWithProfiles(contributors, importer, activationContext);
		// 将解析到的配置文件中的属性添加到环境中，即配置文件生效
		// 将 contributors 中标记为 BOUND_IMPORT 的配置数据添加到当前项目的运行环境中
		applyToEnvironment(contributors, activationContext, importer.getLoadedLocations(),
				importer.getOptionalLocations());
	}

	private ConfigDataEnvironmentContributors processInitial(ConfigDataEnvironmentContributors contributors,
			ConfigDataImporter importer) {
		this.logger.trace("Processing initial config data environment contributors without activation context");
		contributors = contributors.withProcessedImports(importer, null);
		registerBootstrapBinder(contributors, null, DENY_INACTIVE_BINDING);
		return contributors;
	}

	private ConfigDataActivationContext createActivationContext(Binder initialBinder) {
		this.logger.trace("Creating config data activation context from initial contributions");
		try {
			return new ConfigDataActivationContext(this.environment, initialBinder);
		}
		catch (BindException ex) {
			if (ex.getCause() instanceof InactiveConfigDataAccessException) {
				throw (InactiveConfigDataAccessException) ex.getCause();
			}
			throw ex;
		}
	}

	private ConfigDataEnvironmentContributors processWithoutProfiles(ConfigDataEnvironmentContributors contributors,
			ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
		this.logger.trace("Processing config data environment contributors with initial activation context");
		contributors = contributors.withProcessedImports(importer, activationContext);
		registerBootstrapBinder(contributors, activationContext, DENY_INACTIVE_BINDING);
		return contributors;
	}

	private ConfigDataActivationContext withProfiles(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext) {
		this.logger.trace("Deducing profiles from current config data environment contributors");
		Binder binder = contributors.getBinder(activationContext,
				(contributor) -> !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES),
				BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		try {
			Set<String> additionalProfiles = new LinkedHashSet<>(this.additionalProfiles);
			additionalProfiles.addAll(getIncludedProfiles(contributors, activationContext));
			Profiles profiles = new Profiles(this.environment, binder, additionalProfiles);
			return activationContext.withProfiles(profiles);
		}
		catch (BindException ex) {
			if (ex.getCause() instanceof InactiveConfigDataAccessException) {
				throw (InactiveConfigDataAccessException) ex.getCause();
			}
			throw ex;
		}
	}

	private Collection<? extends String> getIncludedProfiles(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext) {
		PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
				contributors, activationContext, null, true);
		Set<String> result = new LinkedHashSet<>();
		for (ConfigDataEnvironmentContributor contributor : contributors) {
			ConfigurationPropertySource source = contributor.getConfigurationPropertySource();
			if (source != null && !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES)) {
				Binder binder = new Binder(Collections.singleton(source), placeholdersResolver);
				binder.bind(Profiles.INCLUDE_PROFILES, STRING_LIST).ifBound((includes) -> {
					if (!contributor.isActive(activationContext)) {
						InactiveConfigDataAccessException.throwIfPropertyFound(contributor, Profiles.INCLUDE_PROFILES);
						InactiveConfigDataAccessException.throwIfPropertyFound(contributor,
								Profiles.INCLUDE_PROFILES.append("[0]"));
					}
					result.addAll(includes);
				});
			}
		}
		return result;
	}

	private ConfigDataEnvironmentContributors processWithProfiles(ConfigDataEnvironmentContributors contributors,
			ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
		this.logger.trace("Processing config data environment contributors with profile activation context");
		contributors = contributors.withProcessedImports(importer, activationContext);
		registerBootstrapBinder(contributors, activationContext, ALLOW_INACTIVE_BINDING);
		return contributors;
	}

	private void registerBootstrapBinder(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext, BinderOption... binderOptions) {
		this.bootstrapContext.register(Binder.class,
				InstanceSupplier.from(() -> contributors.getBinder(activationContext, binderOptions))
					.withScope(Scope.PROTOTYPE));
	}

	private void applyToEnvironment(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext, Set<ConfigDataLocation> loadedLocations,
			Set<ConfigDataLocation> optionalLocations) {
		checkForInvalidProperties(contributors);
		checkMandatoryLocations(contributors, activationContext, loadedLocations, optionalLocations);
		MutablePropertySources propertySources = this.environment.getPropertySources();
		applyContributor(contributors, activationContext, propertySources);
		DefaultPropertiesPropertySource.moveToEnd(propertySources);
		Profiles profiles = activationContext.getProfiles();
		this.logger.trace(LogMessage.format("Setting default profiles: %s", profiles.getDefault()));
		this.environment.setDefaultProfiles(StringUtils.toStringArray(profiles.getDefault()));
		this.logger.trace(LogMessage.format("Setting active profiles: %s", profiles.getActive()));
		this.environment.setActiveProfiles(StringUtils.toStringArray(profiles.getActive()));
		this.environmentUpdateListener.onSetProfiles(profiles);
	}

	private void applyContributor(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext, MutablePropertySources propertySources) {
		this.logger.trace("Applying config data environment contributions");
		for (ConfigDataEnvironmentContributor contributor : contributors) {
			PropertySource<?> propertySource = contributor.getPropertySource();
			if (contributor.getKind() == ConfigDataEnvironmentContributor.Kind.BOUND_IMPORT && propertySource != null) {
				if (!contributor.isActive(activationContext)) {
					this.logger
						.trace(LogMessage.format("Skipping inactive property source '%s'", propertySource.getName()));
				}
				else {
					this.logger
						.trace(LogMessage.format("Adding imported property source '%s'", propertySource.getName()));
					propertySources.addLast(propertySource);
					this.environmentUpdateListener.onPropertySourceAdded(propertySource, contributor.getLocation(),
							contributor.getResource());
				}
			}
		}
	}

	private void checkForInvalidProperties(ConfigDataEnvironmentContributors contributors) {
		for (ConfigDataEnvironmentContributor contributor : contributors) {
			InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor);
		}
	}

	private void checkMandatoryLocations(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext, Set<ConfigDataLocation> loadedLocations,
			Set<ConfigDataLocation> optionalLocations) {
		Set<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>();
		for (ConfigDataEnvironmentContributor contributor : contributors) {
			if (contributor.isActive(activationContext)) {
				mandatoryLocations.addAll(getMandatoryImports(contributor));
			}
		}
		for (ConfigDataEnvironmentContributor contributor : contributors) {
			if (contributor.getLocation() != null) {
				mandatoryLocations.remove(contributor.getLocation());
			}
		}
		mandatoryLocations.removeAll(loadedLocations);
		mandatoryLocations.removeAll(optionalLocations);
		if (!mandatoryLocations.isEmpty()) {
			for (ConfigDataLocation mandatoryLocation : mandatoryLocations) {
				this.notFoundAction.handle(this.logger, new ConfigDataLocationNotFoundException(mandatoryLocation));
			}
		}
	}

	private Set<ConfigDataLocation> getMandatoryImports(ConfigDataEnvironmentContributor contributor) {
		List<ConfigDataLocation> imports = contributor.getImports();
		Set<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>(imports.size());
		for (ConfigDataLocation location : imports) {
			if (!location.isOptional()) {
				mandatoryLocations.add(location);
			}
		}
		return mandatoryLocations;
	}

}
