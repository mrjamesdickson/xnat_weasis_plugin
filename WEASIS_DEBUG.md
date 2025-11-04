# Weasis Debug Logging Configuration

## Enable Detailed Logging

To troubleshoot Weasis DICOM viewer issues, you can enable detailed debug logging.

### Steps

1. **Locate the Weasis preferences file:**
   - **Windows:** `%USERPROFILE%\.weasis\preferences\<username>\default\weasis.properties`
   - **macOS/Linux:** `~/.weasis/preferences/<username>/default/weasis.properties`

2. **Edit the logging level:**
   Open the file and change:
   ```properties
   org.apache.sling.commons.log.level=INFO
   ```
   To:
   ```properties
   org.apache.sling.commons.log.level=TRACE
   ```

   Available log levels (from least to most verbose):
   - `ERROR` - Only errors
   - `WARN` - Warnings and errors
   - `INFO` - General information (default)
   - `DEBUG` - Detailed debugging information
   - `TRACE` - Very detailed trace information

3. **Save the file and restart Weasis**

4. **View the logs:**
   - **Windows:** `%USERPROFILE%\.weasis\log\default.log`
   - **macOS/Linux:** `~/.weasis/log/default.log`

### What to Look For

When troubleshooting XNAT integration issues, check the logs for:
- HTTP request URLs and responses
- Authentication errors (401, 403 status codes)
- DICOMweb endpoint connectivity
- DICOM instance download progress
- Exception stack traces

### Reverting to Normal Logging

After debugging, change the log level back to `INFO` to reduce log file size:
```properties
org.apache.sling.commons.log.level=INFO
```
