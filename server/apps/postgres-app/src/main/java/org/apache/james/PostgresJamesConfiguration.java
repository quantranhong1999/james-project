/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james;

import java.io.File;
import java.util.Optional;

import org.apache.james.data.UsersRepositoryModuleChooser;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.modules.blobstore.BlobStoreConfiguration;
import org.apache.james.server.core.JamesServerResourceLoader;
import org.apache.james.server.core.MissingArgumentException;
import org.apache.james.server.core.configuration.Configuration;
import org.apache.james.server.core.configuration.FileConfigurationProvider;
import org.apache.james.server.core.filesystem.FileSystemImpl;
import org.apache.james.utils.PropertiesProvider;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Preconditions;

public class PostgresJamesConfiguration implements Configuration {
    private static BlobStoreConfiguration.BlobStoreImplName DEFAULT_BLOB_STORE = BlobStoreConfiguration.BlobStoreImplName.FILE;

    public static class Builder {
        private Optional<String> rootDirectory;
        private Optional<ConfigurationPath> configurationPath;
        private Optional<UsersRepositoryModuleChooser.Implementation> usersRepositoryImplementation;
        private Optional<SearchConfiguration> searchConfiguration;
        private Optional<BlobStoreConfiguration> blobStoreConfiguration;

        private Builder() {
            searchConfiguration = Optional.empty();
            rootDirectory = Optional.empty();
            configurationPath = Optional.empty();
            usersRepositoryImplementation = Optional.empty();
            blobStoreConfiguration = Optional.empty();
        }

        public Builder workingDirectory(String path) {
            rootDirectory = Optional.of(path);
            return this;
        }

        public Builder workingDirectory(File file) {
            rootDirectory = Optional.of(file.getAbsolutePath());
            return this;
        }

        public Builder useWorkingDirectoryEnvProperty() {
            rootDirectory = Optional.ofNullable(System.getProperty(WORKING_DIRECTORY));
            if (!rootDirectory.isPresent()) {
                throw new MissingArgumentException("Server needs a working.directory env entry");
            }
            return this;
        }

        public Builder configurationPath(ConfigurationPath path) {
            configurationPath = Optional.of(path);
            return this;
        }

        public Builder configurationFromClasspath() {
            configurationPath = Optional.of(new ConfigurationPath(FileSystem.CLASSPATH_PROTOCOL));
            return this;
        }

        public Builder usersRepository(UsersRepositoryModuleChooser.Implementation implementation) {
            this.usersRepositoryImplementation = Optional.of(implementation);
            return this;
        }

        public Builder searchConfiguration(SearchConfiguration searchConfiguration) {
            this.searchConfiguration = Optional.of(searchConfiguration);
            return this;
        }

        public Builder blobStore(BlobStoreConfiguration blobStoreConfiguration) {
            this.blobStoreConfiguration = Optional.of(blobStoreConfiguration);
            return this;
        }

        public PostgresJamesConfiguration build() {
            ConfigurationPath configurationPath = this.configurationPath.orElse(new ConfigurationPath(FileSystem.FILE_PROTOCOL_AND_CONF));
            JamesServerResourceLoader directories = new JamesServerResourceLoader(rootDirectory
                .orElseThrow(() -> new MissingArgumentException("Server needs a working.directory env entry")));

            FileSystemImpl fileSystem = new FileSystemImpl(directories);
            PropertiesProvider propertiesProvider = new PropertiesProvider(fileSystem, configurationPath);

            SearchConfiguration searchConfiguration = this.searchConfiguration.orElseGet(Throwing.supplier(
                () -> SearchConfiguration.parse(propertiesProvider)));

            BlobStoreConfiguration blobStoreConfiguration = this.blobStoreConfiguration.orElseGet(Throwing.supplier(
                () -> BlobStoreConfiguration.parse(propertiesProvider, DEFAULT_BLOB_STORE)));
            Preconditions.checkState(!blobStoreConfiguration.getImplementation().equals(BlobStoreConfiguration.BlobStoreImplName.CASSANDRA), "Cassandra BlobStore is not supported by postgres-app.");
            Preconditions.checkState(!blobStoreConfiguration.cacheEnabled(), "BlobStore caching is not supported by postgres-app.");

            FileConfigurationProvider configurationProvider = new FileConfigurationProvider(fileSystem, Basic.builder()
                .configurationPath(configurationPath)
                .workingDirectory(directories.getRootDirectory())
                .build());
            UsersRepositoryModuleChooser.Implementation usersRepositoryChoice = usersRepositoryImplementation.orElseGet(
                () -> UsersRepositoryModuleChooser.Implementation.parse(configurationProvider));

            return new PostgresJamesConfiguration(
                configurationPath,
                directories,
                searchConfiguration,
                usersRepositoryChoice,
                blobStoreConfiguration);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final ConfigurationPath configurationPath;
    private final JamesDirectoriesProvider directories;
    private final SearchConfiguration searchConfiguration;
    private final UsersRepositoryModuleChooser.Implementation usersRepositoryImplementation;
    private final BlobStoreConfiguration blobStoreConfiguration;

    private PostgresJamesConfiguration(ConfigurationPath configurationPath,
                                       JamesDirectoriesProvider directories,
                                       SearchConfiguration searchConfiguration,
                                       UsersRepositoryModuleChooser.Implementation usersRepositoryImplementation,
                                       BlobStoreConfiguration blobStoreConfiguration) {
        this.configurationPath = configurationPath;
        this.directories = directories;
        this.searchConfiguration = searchConfiguration;
        this.usersRepositoryImplementation = usersRepositoryImplementation;
        this.blobStoreConfiguration = blobStoreConfiguration;
    }

    @Override
    public ConfigurationPath configurationPath() {
        return configurationPath;
    }

    @Override
    public JamesDirectoriesProvider directories() {
        return directories;
    }

    public SearchConfiguration searchConfiguration() {
        return searchConfiguration;
    }

    public UsersRepositoryModuleChooser.Implementation getUsersRepositoryImplementation() {
        return usersRepositoryImplementation;
    }

    public BlobStoreConfiguration blobStoreConfiguration() {
        return blobStoreConfiguration;
    }
}
