chrome.tabs.query(
    {
        active: true,
        currentWindow: true
    },
    async function (tabs) {

        const currentTab = tabs[0];
        const currentUrl = currentTab.url;

        document.getElementById("url").textContent = currentUrl;

        try {

            const response = await fetch(
                "http://localhost:5000/analyze",
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "application/json"
                    },

                    body: JSON.stringify({
                        url: currentUrl
                    })
                }
            );

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || "Analysis request failed");
            }

            console.log("Backend response:", result);

            document.querySelector(".status-box").innerHTML = `
                <h2>🛡️ ${result.severity} RISK</h2>

                <p>
                    Security Score: <strong>${result.score}/100</strong>
                </p>

                <p>
                    ${result.summary}
                </p>
            `;

        } catch (error) {

            console.error(error);

            document.querySelector(".status-box").innerHTML = `
                <h2>❌ Connection Error</h2>

                <p>
                    GuardianAI could not connect to the backend.
                </p>
            `;
        }
    }
);