package org.nrg.xnat.weasis.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@XapiRestController
@Api("Weasis DICOM Viewer Integration API")
public class WeasisLaunchApi extends AbstractXapiRestController {
    private static final Logger log = LoggerFactory.getLogger(WeasisLaunchApi.class);

    @Autowired
    public WeasisLaunchApi(final UserManagementServiceI userManagementService,
                           final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
    }

    @XapiRequestMapping(value = "/weasis/launch/projects/{projectId}/sessions/{sessionId}",
                        method = RequestMethod.GET,
                        produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Generate Weasis viewer launch URL for an imaging session",
                  notes = "Returns a weasis:// protocol URL to launch the Weasis viewer for the specified session")
    public ResponseEntity<String> launchSession(@PathVariable final String projectId,
                                                @PathVariable final String sessionId,
                                                final HttpServletRequest request) {
        log.debug("Weasis launch requested for projectId={} sessionId={}", projectId, sessionId);

        final UserI user = XDAT.getUserDetails();
        if (user == null) {
            log.warn("Rejected Weasis launch request for projectId={} sessionId={} due to missing user session", projectId, sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(sessionId, user, false);
        if (session == null) {
            log.warn("Weasis launch request for projectId={} sessionId={} not found or not visible to user {}", projectId, sessionId, user.getUsername());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not found");
        }

        final String sessionProject = session.getProject();
        final boolean matchesProject = (sessionProject != null && sessionProject.equalsIgnoreCase(projectId))
                                       || session.hasProject(projectId);
        if (!matchesProject) {
            log.warn("Weasis launch request for projectId={} sessionId={} denied; session belongs to project {}", projectId, sessionId, sessionProject);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Session does not belong to project");
        }

        final String studyInstanceUid = session.getUid();
        if (studyInstanceUid == null || studyInstanceUid.trim().isEmpty()) {
            log.warn("Weasis launch request for projectId={} sessionId={} failed; no study UID found", projectId, sessionId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Session has no study UID");
        }

        try {
            final String weasisUrl = buildWeasisUrl(request, projectId, studyInstanceUid);
            log.info("Generated Weasis launch URL for projectId={} sessionId={} studyUID={}", projectId, sessionId, studyInstanceUid);
            return ResponseEntity.ok(weasisUrl);
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode Weasis URL for projectId={} sessionId={}", projectId, sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating launch URL");
        }
    }

    private String buildWeasisUrl(final HttpServletRequest request, final String projectId, final String studyInstanceUid)
            throws UnsupportedEncodingException {

        final String baseUrl = buildBaseUrl(request);
        final String pathPrefix = buildPathPrefix(request);

        // Construct DICOMweb URL
        final String dicomwebUrl = baseUrl + pathPrefix + "/xapi/dicomweb/projects/" + projectId;

        // Get JSESSIONID for authentication
        final String sessionId = request.getSession().getId();

        // Build Weasis protocol URL
        // weasis://?$dicom:rs --url "DICOMWEB_URL" -r "studyUID=XXX" --header "Cookie: JSESSIONID=YYY"
        final StringBuilder weasisUrl = new StringBuilder("weasis://?");
        weasisUrl.append("$dicom:rs");
        weasisUrl.append(" --url \"").append(dicomwebUrl).append("\"");
        weasisUrl.append(" -r \"studyUID=").append(studyInstanceUid).append("\"");
        weasisUrl.append(" --header \"Cookie: JSESSIONID=").append(sessionId).append("\"");

        return weasisUrl.toString();
    }

    private static String buildBaseUrl(final HttpServletRequest request) {
        String scheme = firstForwardedValue(request, "X-Forwarded-Proto");
        if (scheme == null || scheme.isEmpty()) {
            scheme = request.getScheme();
        }

        String host = request.getServerName();
        int port = request.getServerPort();

        final String forwardedHost = firstForwardedValue(request, "X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isEmpty()) {
            final int colonIndex = forwardedHost.lastIndexOf(':');
            if (colonIndex > 0) {
                host = forwardedHost.substring(0, colonIndex);
                try {
                    port = Integer.parseInt(forwardedHost.substring(colonIndex + 1));
                } catch (NumberFormatException e) {
                    // ignore, keep existing port
                }
            } else {
                host = forwardedHost;
            }
        }

        final String forwardedPort = firstForwardedValue(request, "X-Forwarded-Port");
        if (forwardedPort != null && !forwardedPort.isEmpty()) {
            try {
                port = Integer.parseInt(forwardedPort);
            } catch (NumberFormatException e) {
                // ignore, keep existing port
            }
        }

        final boolean isDefaultPort = (scheme.equalsIgnoreCase("http") && port == 80)
                                      || (scheme.equalsIgnoreCase("https") && port == 443);
        final String portSection = (isDefaultPort || port <= 0) ? "" : ":" + port;

        return scheme + "://" + host + portSection;
    }

    private static String buildPathPrefix(final HttpServletRequest request) {
        final String forwardedPrefix = firstForwardedValue(request, "X-Forwarded-Prefix");
        final String contextPathRaw = request.getContextPath() == null ? "" : request.getContextPath();

        final StringBuilder builder = new StringBuilder();

        if (forwardedPrefix != null && !forwardedPrefix.isEmpty()) {
            String prefix = forwardedPrefix.trim();
            if (!prefix.startsWith("/")) {
                prefix = "/" + prefix;
            }
            if (prefix.endsWith("/")) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            builder.append(prefix);
        }

        if (contextPathRaw != null && !contextPathRaw.isEmpty() && !contextPathRaw.equals("/")) {
            if (builder.length() == 0) {
                builder.append(contextPathRaw);
            } else {
                builder.append(contextPathRaw.startsWith("/") ? contextPathRaw : "/" + contextPathRaw);
            }
        }

        return builder.length() == 0 ? "" : builder.toString();
    }

    private static String firstForwardedValue(final HttpServletRequest request, final String headerName) {
        final String raw = request.getHeader(headerName);
        if (raw == null) {
            return null;
        }
        final int commaIndex = raw.indexOf(',');
        return (commaIndex >= 0 ? raw.substring(0, commaIndex) : raw).trim();
    }
}
