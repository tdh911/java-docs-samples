/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.translate;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TranslateV3BatchTranslateTextWithGlossaryAndModelTest {
  private static final String PROJECT_ID = System.getenv("GOOGLE_PROJECT_ID");
  private static final String INPUT_URI =
          "gs://cloud-samples-data/translation/text_with_custom_model_and_glossary.txt";
  private static final String MODEL_ID = "TRL2188848820815848149";
  private static final String GLOSSARY_INPUT_URI =
          "gs://cloud-samples-data/translation/glossary_ja.csv";

  private String glossaryId;
  private ByteArrayOutputStream bout;
  private PrintStream out;

  private static final void cleanUpBucket() {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Page<Blob> blobs =
            storage.list(
                    PROJECT_ID,
                    Storage.BlobListOption.currentDirectory(),
                    Storage.BlobListOption.prefix("BATCH_TRANSLATION_OUTPUT/"));

    deleteDirectory(storage, blobs);
  }

  private static void deleteDirectory(Storage storage, Page<Blob> blobs) {
    for (Blob blob : blobs.iterateAll()) {
      System.out.println(blob.getBlobId());
      if (!blob.delete()) {
        Page<Blob> subBlobs =
                storage.list(
                        PROJECT_ID,
                        Storage.BlobListOption.currentDirectory(),
                        Storage.BlobListOption.prefix(blob.getName()));

        deleteDirectory(storage, subBlobs);
      }
    }
  }

  @Before
  public void setUp() {
    glossaryId = String.format("must_start_with_letter_%s",
            UUID.randomUUID().toString().replace("-", "_").substring(0, 26));

    // Setup
    TranslateV3CreateGlossary.sampleCreateGlossary(
            PROJECT_ID, glossaryId, GLOSSARY_INPUT_URI);
    bout = new ByteArrayOutputStream();
    out = new PrintStream(bout);
    System.setOut(out);
  }

  @After
  public void tearDown() {
    cleanUpBucket();
    TranslateV3DeleteGlossary.sampleDeleteGlossary(PROJECT_ID, glossaryId);
    System.setOut(null);
  }

  @Test
  public void testBatchTranslateTextWithGlossaryAndModel() {
    // Act
    TranslateV3BatchTranslateTextWithGlossaryAndModel.sampleBatchTranslateTextWithGlossaryAndModel(
            INPUT_URI,
            "gs://" + PROJECT_ID + "/BATCH_TRANSLATION_OUTPUT/",
            PROJECT_ID,
            "us-central1",
            "ja",
            "en",
            MODEL_ID,
            glossaryId
    );

    // Assert
    String got = bout.toString();
    assertThat(got).contains("Total Characters: 25");
  }
}