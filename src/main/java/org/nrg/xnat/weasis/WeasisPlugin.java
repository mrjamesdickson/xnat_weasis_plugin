package org.nrg.xnat.weasis;

import org.nrg.framework.annotations.XnatPlugin;
import org.springframework.context.annotation.ComponentScan;

@XnatPlugin(
        value = "xnat-weasis-plugin",
        name = "XNAT Weasis Plugin",
        description = "Integrates Weasis DICOM viewer using the XNAT DICOMweb proxy.",
        entityPackages = "org.nrg.xnat.weasis",
        openUrls = {
                "/xapi/weasis/launch/**"
        }
)
@ComponentScan({"org.nrg.xnat.weasis"})
public class WeasisPlugin {
}
