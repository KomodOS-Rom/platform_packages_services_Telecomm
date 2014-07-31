/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.android.telecomm.tests.unit;

import com.android.internal.util.FastXmlSerializer;
import com.android.telecomm.Log;
import com.android.telecomm.PhoneAccountRegistrar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.content.ComponentName;
import android.net.Uri;
import android.telecomm.PhoneAccount;
import android.telecomm.PhoneAccountHandle;
import android.test.AndroidTestCase;
import android.util.Xml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class PhoneAccountRegistrarTest extends AndroidTestCase {

    private static final String FILE_NAME = "phone-account-registrar-test.xml";
    private PhoneAccountRegistrar mRegistrar;

    @Override
    public void setUp() {
        mRegistrar = new PhoneAccountRegistrar(getContext(), FILE_NAME);
        mRegistrar.registerPhoneAccount(new PhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id0"),
                Uri.parse("tel:555-1212"),
                "555-1212",
                PhoneAccount.CAPABILITY_CONNECTION_MANAGER,
                0,
                "label0",
                "desc0"));
        mRegistrar.registerPhoneAccount(new PhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id1"),
                Uri.parse("tel:555-1212"),
                "555-1212",
                PhoneAccount.CAPABILITY_CALL_PROVIDER | PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION,
                0,
                "label1",
                "desc1"));
        mRegistrar.registerPhoneAccount(new PhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg1", "cls1"), "id2"),
                Uri.parse("tel:555-1212"),
                "555-1212",
                PhoneAccount.CAPABILITY_CALL_PROVIDER,
                0,
                "label2",
                "desc2"));
        mRegistrar.registerPhoneAccount(new PhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg1", "cls1"), "id3"),
                Uri.parse("tel:555-1212"),
                "555-1212",
                PhoneAccount.CAPABILITY_CALL_PROVIDER,
                0,
                "label2",
                "desc2"));
    }

    @Override
    public void tearDown() {
        mRegistrar = null;
        new File(getContext().getFilesDir(), FILE_NAME).delete();
    }

    private static <T> T roundTrip(
            Object self,
            T input,
            PhoneAccountRegistrar.XmlSerialization<T> xml)
            throws Exception {
        Log.d(self, "Input = %s", input);

        byte[] data;
        {
            XmlSerializer serializer = new FastXmlSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.setOutput(new BufferedOutputStream(baos), "utf-8");
            xml.writeToXml(input, serializer);
            serializer.flush();
            data = baos.toByteArray();
        }

        Log.d(self, "====== XML data ======\n%s", new String(data));

        T result = null;
        {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new BufferedInputStream(new ByteArrayInputStream(data)), null);
            parser.nextTag();
            result = xml.readFromXml(parser);
        }

        Log.d(self, "result = " + result);

        return result;
    }

    private void assertPhoneAccountHandleEquals(PhoneAccountHandle a, PhoneAccountHandle b) {
        if (a != b) {
            assertEquals(
                    a.getComponentName().getPackageName(),
                    b.getComponentName().getPackageName());
            assertEquals(
                    a.getComponentName().getClassName(),
                    b.getComponentName().getClassName());
            assertEquals(a.getId(), b.getId());
        }
    }

    public void testPhoneAccountHandle() throws Exception {
        PhoneAccountHandle input = new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id0");
        PhoneAccountHandle result = roundTrip(this, input, PhoneAccountRegistrar.sPhoneAccountHandleXml);
        assertPhoneAccountHandleEquals(input, result);
        PhoneAccountHandle inputN =
                new PhoneAccountHandle(
                        new ComponentName(
                                "pkg0",  // ctor does not allow null
                                "cls0"), // ctor does not allow null
                        null);
        PhoneAccountHandle resultN = roundTrip(this, inputN, PhoneAccountRegistrar.sPhoneAccountHandleXml);
        Log.i(this, "inputN = %s, resultN = %s", inputN, resultN);
        assertPhoneAccountHandleEquals(inputN, resultN);
    }

    private void assertPhoneAccountEquals(PhoneAccount a, PhoneAccount b) {
        if (a != b) {
            assertPhoneAccountHandleEquals(a.getAccountHandle(), b.getAccountHandle());
            assertEquals(a.getHandle(), b.getHandle());
            assertEquals(a.getSubscriptionNumber(), b.getSubscriptionNumber());
            assertEquals(a.getCapabilities(), b.getCapabilities());
            assertEquals(a.getIconResId(), b.getIconResId());
            assertEquals(a.getLabel(), b.getLabel());
            assertEquals(a.getShortDescription(), b.getShortDescription());
        }
    }

    public void testPhoneAccount() throws Exception {
        PhoneAccount input = makeQuickAccount("pkg0", "cls0", "id0", 0);
        PhoneAccount result = roundTrip(this, input, PhoneAccountRegistrar.sPhoneAccountXml);
        assertPhoneAccountEquals(input, result);
        PhoneAccount inputN = new PhoneAccount(null, null, null, 0, 0, null, null);
        PhoneAccount resultN = roundTrip(this, inputN, PhoneAccountRegistrar.sPhoneAccountXml);
        assertPhoneAccountEquals(inputN, resultN);
    }

    private void assertStateEquals(PhoneAccountRegistrar.State a, PhoneAccountRegistrar.State b) {
        assertPhoneAccountHandleEquals(a.defaultOutgoing, b.defaultOutgoing);
        assertPhoneAccountHandleEquals(a.simCallManager, b.simCallManager);
        assertEquals(a.accounts.size(), b.accounts.size());
        for (int i = 0; i < a.accounts.size(); i++) {
            assertPhoneAccountEquals(a.accounts.get(i), b.accounts.get(i));
        }
    }

    public void testState() throws Exception {
        PhoneAccountRegistrar.State input = makeQuickState();
        PhoneAccountRegistrar.State result = roundTrip(this, input, PhoneAccountRegistrar.sStateXml);
        assertStateEquals(input, result);
    }

    public void testAccounts() throws Exception {
        assertEquals(4, mRegistrar.getAllPhoneAccountHandles().size());
        assertEquals(3, mRegistrar.getEnabledPhoneAccounts().size());
        assertEquals(null, mRegistrar.getSimCallManager());
        assertEquals(null, mRegistrar.getDefaultOutgoingPhoneAccount());
    }

    public void testSimCallManager() throws Exception {
        // Establish initial conditions
        assertEquals(null, mRegistrar.getSimCallManager());
        PhoneAccountHandle h = new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id0");
        mRegistrar.setSimCallManager(h);
        assertPhoneAccountHandleEquals(h, mRegistrar.getSimCallManager());
        mRegistrar.unregisterPhoneAccount(h);
        // If account is un-registered, querying returns null
        assertEquals(null, mRegistrar.getSimCallManager());
        // But if account is re-registered, setting comes back
        mRegistrar.registerPhoneAccount(makeQuickAccount("pkg0", "cls0", "id0", 99));
        assertPhoneAccountHandleEquals(h, mRegistrar.getSimCallManager());
        // De-register by setting to null
        mRegistrar.setSimCallManager(null);
        assertEquals(null, mRegistrar.getSimCallManager());
        // If argument not have SIM_CALL_MANAGER capability, this is a no-op
        mRegistrar.setSimCallManager(
                new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id1"));
        assertEquals(null, mRegistrar.getSimCallManager());
    }

    public void testDefaultOutgoing() {
        // Establish initial conditions
        assertEquals(null, mRegistrar.getDefaultOutgoingPhoneAccount());
        PhoneAccountHandle h = new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id1");
        mRegistrar.setDefaultOutgoingPhoneAccount(h);
        assertPhoneAccountHandleEquals(h, mRegistrar.getDefaultOutgoingPhoneAccount());
        // If account is un-registered, querying returns null
        mRegistrar.unregisterPhoneAccount(h);
        assertEquals(null, mRegistrar.getDefaultOutgoingPhoneAccount());
        // But if account is re-registered, setting comes back
        mRegistrar.registerPhoneAccount(makeQuickAccount("pkg0", "cls0", "id1", 99));
        assertPhoneAccountHandleEquals(h, mRegistrar.getDefaultOutgoingPhoneAccount());
        // De-register by setting to null
        mRegistrar.setDefaultOutgoingPhoneAccount(null);
        assertEquals(null, mRegistrar.getDefaultOutgoingPhoneAccount());
        // If argument not have CALL_PROVIDER capability, this is a no-op
        mRegistrar.setDefaultOutgoingPhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id0"));
        assertEquals(null, mRegistrar.getDefaultOutgoingPhoneAccount());
        // If only have one account, it is the default
        mRegistrar.unregisterPhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id0"));
        mRegistrar.unregisterPhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id1"));
        mRegistrar.unregisterPhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg1", "cls1"), "id2"));
        assertPhoneAccountHandleEquals(
                new PhoneAccountHandle(new ComponentName("pkg1", "cls1"), "id3"),
                mRegistrar.getDefaultOutgoingPhoneAccount());
        // If have one account but not suitable, default returns null
        mRegistrar.unregisterPhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg1", "cls1"), "id3"));
        mRegistrar.registerPhoneAccount(new PhoneAccount(
                new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id0"),
                Uri.parse("tel:555-1212"),
                "555-1212",
                PhoneAccount.CAPABILITY_CONNECTION_MANAGER,
                0,
                "label0",
                "desc0"));
        assertEquals(null, mRegistrar.getDefaultOutgoingPhoneAccount());
    }

    private static PhoneAccount makeQuickAccount(String pkg, String cls, String id, int idx) {
        return new PhoneAccount(
                new PhoneAccountHandle(new ComponentName(pkg, cls), id),
                Uri.parse("http://foo.com/" + idx),
                "555-000" + idx,
                idx,
                idx,
                "label" + idx,
                "desc" + idx);
    }

    private static PhoneAccountRegistrar.State makeQuickState() {
        PhoneAccountRegistrar.State s = new PhoneAccountRegistrar.State();
        s.accounts.add(makeQuickAccount("pkg0", "cls0", "id0", 0));
        s.accounts.add(makeQuickAccount("pkg0", "cls0", "id1", 1));
        s.accounts.add(makeQuickAccount("pkg1", "cls1", "id2", 2));
        s.defaultOutgoing = new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id0");
        s.simCallManager = new PhoneAccountHandle(new ComponentName("pkg0", "cls0"), "id1");
        return s;
    }
}