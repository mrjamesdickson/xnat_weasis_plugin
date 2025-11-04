# XNAT Weasis Plugin

This plugin integrates the Weasis DICOM viewer with XNAT by providing launch URLs that connect Weasis to XNAT's DICOMweb endpoints. Users can launch Weasis directly from XNAT imaging session pages to view studies using the `weasis://` protocol.

Modeled after the [xnat_volview_plugin](../xnat_volview_plugin), this plugin provides a minimal REST API that generates Weasis launch URLs with proper authentication and DICOMweb endpoint configuration.

> **Prerequisites:** XNAT 1.9.0+, Java 8+, the XNAT DICOMweb proxy plugin deployed and configured, and users must have Weasis 3.5.3+ installed locally.

---

## Features

- Launches Weasis DICOM viewer from XNAT imaging sessions
- Uses XNAT's DICOMweb endpoints for secure image retrieval
- Session-based authentication via JSESSIONID cookies
- Open source viewer (no licensing costs)
- Zero-footprint web integration (no embedded viewer needed)

---

## Repository Layout

```
.
├── build.gradle
├── settings.gradle
├── src
│   └── main
│       ├── java
│       │   └── org/nrg/xnat/weasis
│       │       ├── WeasisPlugin.java
│       │       └── rest/WeasisLaunchApi.java
│       └── resources
│           └── META-INF
│               ├── xnat/weasis-plugin.properties
│               └── site-config/weasis-plugin.properties
└── README.md
```

---

## Building the Plugin

```bash
./gradlew clean build
```

Output: `build/libs/xnat-weasis-plugin-1.0.0.jar`

---

## Deploying the Plugin

1. Copy the JAR to your XNAT `plugins/` directory
2. Restart XNAT
3. Verify deployment by checking XNAT logs for "Loading plugin: xnat-weasis-plugin"

---

## User Setup

Users must install the Weasis native application:

1. Download from https://weasis.org
2. Run the native installer (registers the `weasis://` protocol handler)
3. Verify installation by testing a `weasis://` link in your browser

---

## Usage

### REST API

The plugin exposes a single endpoint:

**GET** `/xapi/weasis/launch/projects/{projectId}/sessions/{sessionId}`

Returns a `weasis://` protocol URL that launches Weasis with the specified session.

**Example Response:**
```
weasis://?$dicom:rs --url "https://xnat.example.org/xapi/dicomweb/projects/MyProject" -r "studyUID=1.2.840.113619..." --header "Cookie: JSESSIONID=ABC123"
```

### Integration Points

To add a "Launch Weasis" button to XNAT pages, you can:

1. **JavaScript integration** - Call the API endpoint and open the returned URL
2. **Direct link** - Create an anchor tag with `href` pointing to the API endpoint
3. **XNAT Action** - Add a custom action to project or session pages

**Example JavaScript:**
```javascript
fetch('/xapi/weasis/launch/projects/MyProject/sessions/XNAT_E00001')
  .then(response => response.text())
  .then(weasisUrl => {
    window.location.href = weasisUrl;
  });
```

---

## Configuration

Default settings are defined in `META-INF/site-config/weasis-plugin.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `weasis.dicomweb.base-path` | `/xapi/dicomweb/projects` | Base path for DICOMweb endpoints |
| `weasis.server-name` | `XNAT DICOMweb` | Friendly server name |

These can be overridden via XNAT site configuration or environment variables.

---

## How It Works

1. User requests launch URL via `/xapi/weasis/launch/projects/{projectId}/sessions/{sessionId}`
2. Plugin validates user permissions and retrieves session metadata
3. Plugin extracts the Study Instance UID from the XNAT session
4. Plugin builds a `weasis://` protocol URL with:
   - DICOMweb base URL for the project
   - Study UID parameter
   - Session cookie for authentication
5. Browser invokes the `weasis://` protocol handler
6. Weasis launches and connects to XNAT's DICOMweb endpoint
7. Weasis retrieves and displays the study images

---

## Troubleshooting

**Weasis doesn't launch**
- Verify Weasis is installed and the native installer was used
- Test if your browser recognizes the `weasis://` protocol
- Check browser console for protocol handler errors

**Authentication failures**
- Ensure user is logged into XNAT
- Check that session cookies are being passed correctly
- Verify XNAT DICOMweb proxy is installed and configured

**Images don't load**
- Confirm XNAT DICOMweb endpoints are accessible
- Check XNAT logs for DICOMweb proxy errors
- Verify the session has valid DICOM data

---

## Development Notes

- Modeled after the xnat_volview_plugin architecture
- Uses XNAT's `@XnatPlugin` and `@XapiRestController` annotations
- Leverages XNAT's existing DICOMweb infrastructure
- No client-side assets required (protocol-based launch)
- All authentication handled via existing XNAT sessions

---

## Resources

- **Weasis Documentation:** https://weasis.org
- **Weasis Protocol Documentation:** https://weasis.org/en/getting-started/weasis-protocol/
- **XNAT DICOMweb Plugin:** https://github.com/NrgXnat/xnat-dicomweb-plugin
- **VolView Plugin (reference):** ../xnat_volview_plugin
