/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
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

package org.xipki.ocsp.client.api;

import org.xipki.common.util.Hex;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@SuppressWarnings("serial")
public class OcspNonceUnmatchedException extends OcspResponseException {

    public OcspNonceUnmatchedException(byte[] expected, byte[] is) {
        super(buildMessage(expected, is));
    }

    private static String buildMessage(byte[] expected, byte[] is) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("nonce unmatch (received ");
        if (is == null || is.length == 0) {
            sb.append("none");
        } else {
            sb.append(Hex.encode(is));
        }
        sb.append(", but expected ");
        if (expected == null || expected.length == 0) {
            sb.append("nonce");
        } else {
            sb.append(Hex.encode(expected));
        }
        sb.append(")");
        return sb.toString();
    }

}
