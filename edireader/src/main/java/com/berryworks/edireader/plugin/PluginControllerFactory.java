/*
 * Copyright 2005-2015 by BerryWorks Software, LLC. All rights reserved.
 *
 * This file is part of EDIReader. You may obtain a license for its use directly from
 * BerryWorks Software, and you may also choose to use this software under the terms of the
 * GPL version 3. Other products in the EDIReader software suite are available only by licensing
 * with BerryWorks. Only those files bearing the GPL statement below are available under the GPL.
 *
 * EDIReader is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * EDIReader is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EDIReader.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package com.berryworks.edireader.plugin;

import com.berryworks.edireader.Plugin;

public class PluginControllerFactory extends AbstractPluginControllerFactory {

    public static final String DEFAULT_EDIREADER_PLUGIN_PACKAGE = "com.berryworks.edireader.plugin";

    /**
     * Used only within the internal implementation of this class and its subclasses.
     *
     * @param standard - name of standard
     * @param docType  - type of document
     * @return Plugin if search was satisfied, or null if not
     */
    protected static Plugin lookForSpecificPlugin(String standard, String docType) {
        Plugin pluginFound = null;
        try {
            pluginFound = getInstance(standard, docType);
            if (debug) {
                pluginFound.debug(true);
                trace("plugin found for document type " + docType + ": "
                        + pluginFound.getDocumentName());
            }
        } catch (ClassNotFoundException e) {
            if (debug)
                trace("plugin for " + docType + " not available");
        } catch (InstantiationException e) {
            if (debug)
                trace("plugin for " + docType + " could not be instantiated");
        } catch (IllegalAccessException e) {
            if (debug)
                trace("plugin for " + docType + " caused IllegalAccessException" + e);
        }

        return pluginFound;
    }

    /**
     * Used only within the internal implementation of this class and its subclasses.
     *
     * @param standard - name of EDI standard (for example: "EDIFACT" or "ANSI")
     * @param docType  - type of document, possible enhanced with version and release (for example: "203" or "203_X_004010")
     * @return Plugin that was loaded
     * @throws ClassNotFoundException if the class is not present in the classpath
     * @throws InstantiationException for problems when attempting to load the class
     * @throws IllegalAccessException for problems when attempting to load the class
     */
    protected static Plugin getInstance(String standard, String docType) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Plugin instance;
        String pluginName = pluginPackage() + "." + standard + "_" + docType;
        if (debug)
            trace("attempting to load a plugin named " + pluginName);
        Class pluginClass = Class.forName(pluginName);
        if (debug)
            trace("plugin loaded");
        instance = (Plugin) pluginClass.newInstance();
        instance.prepare();
        lastPluginLoaded = pluginName;
        return instance;
    }

    /**
     * Returns the name of the package in which plugins are expected to appear.
     * <p/>
     * The default value is "com.berryworks.edireader.plugin" but this can be changed by setting
     * the system property EDIREADER_PLUGIN_PACKAGE. In this way, a user can develop their own plugins and using
     * a package naming scheme of their choice.
     *
     * @return package name
     */
    protected static String pluginPackage() {
        String packageName = System.getProperty("EDIREADER_PLUGIN_PACKAGE");
        if (packageName == null) {
            packageName = DEFAULT_EDIREADER_PLUGIN_PACKAGE;
            if (debug)
                trace("Plugin package defaults to " + packageName);
        } else {
            if (debug)
                trace("Plugin package set by property to " + packageName);
        }
        return packageName;
    }


}
