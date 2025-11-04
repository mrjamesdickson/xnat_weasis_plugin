(function () {
    function openWeasis(event) {
        const context = XNAT && XNAT.data && XNAT.data.context ? XNAT.data.context : {};
        const projectId = context.projectID || context.projectId;
        const sessionId = context.ID || context.id;

        if (!projectId || !sessionId) {
            XNAT.ui.banner.top(1, 'Unable to determine project or session id for Weasis.');
            return;
        }

        const url = XNAT.url.rootUrl(`/xapi/weasis/launch/projects/${encodeURIComponent(projectId)}/sessions/${encodeURIComponent(sessionId)}`);

        XNAT.xhr.get({
            url: url,
            success: function(weasisUrl) {
                // Create an invisible iframe to launch the protocol handler without URL encoding
                const iframe = document.createElement('iframe');
                iframe.style.display = 'none';
                iframe.src = weasisUrl;
                document.body.appendChild(iframe);
                setTimeout(function() {
                    document.body.removeChild(iframe);
                }, 1000);
            },
            error: function(xhr) {
                const message = xhr.responseText || 'Failed to launch Weasis viewer';
                XNAT.ui.banner.top(5000, message, 'error');
            }
        });
    }

    function handleMouseDown(event) {
        if (event.button === 2) {
            return;
        }
        event.preventDefault();
        openWeasis(event);
    }

    function handleContextMenu(event) {
        event.preventDefault();
        openWeasis(event);
    }

    $(document).on('mousedown', '#weasisViewer', handleMouseDown);
    $(document).on('contextmenu', '#weasisViewer', handleContextMenu);
})();
