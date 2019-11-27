/*
 *   Copyright 2019 contributors to elastic2lmdb
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.whalebone.iprangerjava;

import lombok.Builder;
import lombok.Getter;

/**
 * @author Tomas Kozel
 */

@Getter
@Builder
public class Configuration {
    private long maxEnvSize;
    private String ip4RangesToIdentityDbName;
    private String identitiesToIp4RangesDbName;
    private String ip4MasksDbName;
    private String ip6RangesToIdentityDbName;
    private String identitiesToIp6RangesDbName;
    private String ip6MasksDbName;
    private int maxMaskKeySize;
    private int maxIdentityKeySize;
}
