/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.editor.language.json.converter;

import java.util.Map;

public class StandaloneBpmnConverterContext implements BpmnJsonConverterContext {

    @Override
    public String getFormModelKeyForFormModelId(final String formModelId) {
        return null;
    }

    @Override
    public Map<String, String> getFormModelInfoForFormModelKey(final String formModelKey) {
        return null;
    }

    @Override
    public String getProcessModelKeyForProcessModelId(final String processModelId) {
        return null;
    }

    @Override
    public Map<String, String> getProcessModelInfoForProcessModelKey(final String processModelKey) {
        return null;
    }

    @Override
    public String getDecisionTableModelKeyForDecisionTableModelId(final String decisionTableModelId) {
        return null;
    }

    @Override
    public Map<String, String> getDecisionTableModelInfoForDecisionTableModelKey(final String decisionTableModelKey) {
        return null;
    }

    @Override
    public String getDecisionServiceModelKeyForDecisionServiceModelId(final String decisionServiceModelId) {
        return null;
    }

    @Override
    public Map<String, String> getDecisionServiceModelInfoForDecisionServiceModelKey(
            final String decisionServiceModelKey) {

        return null;
    }
}
