package com.nyefan.snippet.atm.core;

import javax.crypto.Cipher;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

//This should be done with a jar added to the jre in accordance with US export policy
//However, this is more portable and so appropriate for a demonstration
public class KeyLengthOverrider {
    static {
        //credit https://stackoverflow.com/questions/6481627/java-security-illegal-key-size-or-default-parameters
        //probably only works with java 8
        try {
            String errorString = "Failed manually overriding key-length permissions.";
            int oldMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
            if (oldMaxKeyLength < 512) {
                Class CryptoAllPermissionCollection = Class.forName("javax.crypto.CryptoAllPermissionCollection");
                Constructor CryptoAllPermissionsCollection_Constructor = CryptoAllPermissionCollection.getDeclaredConstructor();
                boolean CryptoAllPermissionsCollection_Constructor_isAccessible = CryptoAllPermissionsCollection_Constructor.isAccessible();
                CryptoAllPermissionsCollection_Constructor.setAccessible(true);
                Object allPermissionsCollection = CryptoAllPermissionsCollection_Constructor.newInstance();
                Field CryptoAllPermissionsCollection_all_allowed = CryptoAllPermissionCollection.getDeclaredField("all_allowed");
                boolean CryptoAllPermissionsCollection_all_allowed_isAccessible = CryptoAllPermissionsCollection_all_allowed.isAccessible();
                CryptoAllPermissionsCollection_all_allowed.setAccessible(true);
                CryptoAllPermissionsCollection_all_allowed.setBoolean(allPermissionsCollection, true);

                Class CryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
                Constructor CryptoPermissions_Constructor = CryptoPermissions.getDeclaredConstructor();
                boolean CryptoPermissions_Constructor_isAccessible = CryptoAllPermissionsCollection_Constructor.isAccessible();
                CryptoPermissions_Constructor.setAccessible(true);
                Object allPermissions = CryptoPermissions_Constructor.newInstance();
                Field CryptoPermissions_perms = CryptoPermissions.getDeclaredField("perms");
                boolean CryptoPermissions_perms_isAccessible = CryptoPermissions_perms.isAccessible();
                CryptoPermissions_perms.setAccessible(true);
                ((Map) CryptoPermissions_perms.get(allPermissions)).put("*", allPermissionsCollection);

                Class JceSecurityManager = Class.forName("javax.crypto.JceSecurityManager");
                Field JceSecurityManager_defaultPolicy = JceSecurityManager.getDeclaredField("defaultPolicy");
                boolean JceSecurityManager_defaultPolicy_isAccessible = JceSecurityManager_defaultPolicy.isAccessible();
                JceSecurityManager_defaultPolicy.setAccessible(true);
                Field Field_modifiers = Field.class.getDeclaredField("modifiers");
                boolean Field_modifiers_isAccessible = Field_modifiers.isAccessible();
                Field_modifiers.setAccessible(true);
                //Unset final modifier flag
                Field_modifiers.setInt(JceSecurityManager_defaultPolicy, JceSecurityManager_defaultPolicy.getModifiers() & ~Modifier.FINAL);
                JceSecurityManager_defaultPolicy.set(null, allPermissions);

                //Clean up after ourselves
                CryptoAllPermissionsCollection_Constructor.setAccessible(CryptoAllPermissionsCollection_Constructor_isAccessible);
                CryptoAllPermissionsCollection_all_allowed.setAccessible(CryptoAllPermissionsCollection_all_allowed_isAccessible);
                CryptoPermissions_Constructor.setAccessible(CryptoPermissions_Constructor_isAccessible);
                CryptoPermissions_perms.setAccessible(CryptoPermissions_perms_isAccessible);
                JceSecurityManager_defaultPolicy.setAccessible(JceSecurityManager_defaultPolicy_isAccessible);
                Field_modifiers.setAccessible(Field_modifiers_isAccessible);
            }

            int overriddenMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
            if (overriddenMaxKeyLength < 512) {
                System.out.println("Failed manually overriding key-length permissions.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}
