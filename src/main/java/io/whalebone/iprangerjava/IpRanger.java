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

/**
 * @author Tomas Kozel
 */
public abstract class IpRanger {
    private static IpRanger instance;
    private static volatile boolean init = false;

    public static IpRanger getInstance() {
        init();
        return instance;
    }

    private static void init() {
        if (!init) {
            synchronized (IpRanger.class) {
                if (!init) {
                    try {
                        System.err.println(System.getProperty("java.library.path"));
                        System.loadLibrary("ipranger");
                        instance = new IpRangerImpl();
                    } catch (Throwable t) {
                        throw new RuntimeException("Cannot load ipranger linked C library", t);
                    }
                    init = true;
                }
            }
        }
    }

    public abstract int initDb(String path, boolean readOnly);
}
