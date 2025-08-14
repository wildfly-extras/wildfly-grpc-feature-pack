/*
 *  Copyright The WildFly Authors
 *  SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.feature.pack.grpc;

import java.util.ArrayList;
import java.util.List;

/**
 * Used by {@link ServerInterceptor}s to indicate the order in which they execute.
 *
 */
public class InterceptorTracker {

    private static String flag = "";
    private static List<Integer> list = new ArrayList<Integer>();

    public static synchronized void checkin(String s) {
        flag += s;
    }

    public static synchronized void checkin(Integer i) {
        list.add(i);
    }

    public static synchronized String getFlag() {
        return flag;
    }

    public static synchronized List<Integer> getList() {
        return list;
    }

    public static synchronized void reset() {
        flag = "";
        list = new ArrayList<Integer>();
    }
}
