/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.azure.storage;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processors.azure.AbstractAzureDataLakeStorageProcessor;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;

import java.util.concurrent.TimeUnit;

@Tags({"azure", "microsoft", "cloud", "storage", "adlsgen2", "datalake"})
@SeeAlso({PutAzureDataLakeStorage.class})
@CapabilityDescription("Deletes the provided file from Azure Data Lake Storage")
@InputRequirement(Requirement.INPUT_REQUIRED)
public class DeleteAzureDataLakeStorage extends AbstractAzureDataLakeStorageProcessor {

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();

        if (flowFile == null) {
            return;
        }

        final long startNanos = System.nanoTime();
        try {
            final String fileSystem = context.getProperty(FILESYSTEM).evaluateAttributeExpressions(flowFile).getValue();
            final String directory = context.getProperty(DIRECTORY).evaluateAttributeExpressions(flowFile).getValue();
            final String fileName = context.getProperty(FILE).evaluateAttributeExpressions(flowFile).getValue();
            final DataLakeServiceClient storageClient = getStorageClient(context, flowFile);
            final DataLakeFileSystemClient dataLakeFileSystemClient = storageClient.getFileSystemClient(fileSystem);
            final DataLakeDirectoryClient directoryClient = dataLakeFileSystemClient.getDirectoryClient(directory);

            final DataLakeFileClient fileClient = directoryClient.getFileClient(fileName);
            fileClient.delete();
            session.transfer(flowFile, REL_SUCCESS);

            final long transferMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            session.getProvenanceReporter().send(flowFile, fileClient.getFileUrl(), transferMillis);
        } catch (Exception e) {
            getLogger().error("Failed to delete the specified file from Azure Data Lake Storage,  due to {}", e);
            flowFile = session.penalize(flowFile);
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}
