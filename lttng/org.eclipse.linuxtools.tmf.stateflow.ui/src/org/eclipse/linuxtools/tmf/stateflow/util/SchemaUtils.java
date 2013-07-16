package org.eclipse.linuxtools.tmf.stateflow.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.linuxtools.tmf.stateflow.ui.Activator;

public class SchemaUtils {
	public static void copyEsxStateSchemaToFile( File outputXmlSchemaFile) throws IOException {
		// the super class implementation of buildStateSystem uses a factory method which purely analyzes the
    	// file system to figure out which state system factory to use.  We already know which one to use here,
    	// and so there is no need to use this.  Additionally we want to use the schema file included in the
    	// plugin so that the user doesn't have to provide one with every trace which is annoying.
    	InputStream schemaXmlInputStream = Activator.getResource("resources/esx-vm-events.state-schema.xml");
    	//Object o = new FileUtils.
    	FileUtils.copyInputStreamToFile(schemaXmlInputStream, outputXmlSchemaFile);
	}
}
