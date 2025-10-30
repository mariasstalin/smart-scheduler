document.addEventListener('DOMContentLoaded', function () {
    const messagesList = document.getElementById('messages');
    const input = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');

    let stompClient = null;

    // Helper function to convert newline characters (\n) into HTML line breaks (<br>)
    function formatText(text) {
        if (!text) return '';
        // Replace newline characters with <br> tags for proper rendering
        return text.replace(/\n/g, '<br>');
    }

    function connect() {
        const socket = new SockJS('/demo/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function (frame) {
            console.log('Connected: ' + frame);
            sendBtn.disabled = false;

            // Subscribe to user's private topic
            stompClient.subscribe('/topic/messages-' + userId, function (message) {
                const payload = JSON.parse(message.body);
                // Fix: Ignore messages that originated from this user to prevent echo duplication
                if (payload.from === userId) {
                    return;
                }
                // PASS the current timestamp when received
                showMessage(payload, new Date());
            });
        }, function (error) {
            console.error('Connection error:', error);
            sendBtn.disabled = true;
            setTimeout(connect, 5000);
        });
    }

    function showMessage(payload, receivedTime = new Date()) {
                const li = document.createElement('li');

                // Classify based on whether the sender ID matches the current user ID (userId).
                const fromType = payload.from === userId ? 'from-user' : 'from-system';
                li.className = 'message ' + fromType;

                // Avatar
                const avatar = document.createElement('div');
                avatar.className = 'avatar';
                avatar.textContent = fromType === 'from-system' ? '🤖' : '👤';

                // Message bubble
                const bubble = document.createElement('div');
                bubble.className = 'bubble';

                // ⭐ TIMESTAMP ELEMENT CREATION ⭐
                const timestampEl = document.createElement('span');
                timestampEl.className = 'timestamp';
                // Format the time as HH:MM AM/PM
                timestampEl.textContent = receivedTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });


                let buttonsToRender = [];
                let messageText = payload.body || payload.message || ''; // Default text

                // --- Logic to extract message text and buttons based on format ---

                // 1. Twilio/Interactive Format Check
                if (payload.type === 'interactive' && payload.interactive && payload.interactive.action) {
                    messageText = payload.interactive.body?.text || 'Select an option:';
                    const interactiveButtons = payload.interactive.action.buttons || [];

                    buttonsToRender = interactiveButtons.map(btn => ({
                        id: btn.reply?.id || btn.id,
                        title: btn.reply?.title || btn.title || btn.text || 'Button'
                    }));
                }

                // 2. Standard Rasa Button Check
                else if (payload.buttons && payload.buttons.length > 0) {
                    buttonsToRender = payload.buttons.map(btn => ({
                        id: btn.payload,
                        title: btn.title
                    }));
                }

                // --- Render Content ---

                // Render the main text/HTML content
                // The timestamp will be visually separated by the CSS position:absolute property
                bubble.innerHTML = formatText(messageText);

                if (buttonsToRender.length > 0) {
                    const buttonContainer = document.createElement('div');
                    buttonContainer.className = 'interactive-buttons';

                    buttonsToRender.forEach(btn => {
                        const buttonEl = document.createElement('button');
                        buttonEl.className = 'interactive-button';

                        buttonEl.textContent = btn.title;
                        // Use the 'id' (which is the payload/buttonId) and the title
                        buttonEl.addEventListener('click', () => handleButtonClick(btn.id, btn.title));
                        buttonContainer.appendChild(buttonEl);
                    });

                    bubble.appendChild(buttonContainer);
                }

                // ⭐ APPEND TIMESTAMP ⭐
                bubble.appendChild(timestampEl);

                // --- Existing Appending Logic ---
                li.appendChild(avatar);
                li.appendChild(bubble);
                messagesList.appendChild(li);
                messagesList.scrollTop = messagesList.scrollHeight;
    }

    // When user clicks a button from an interactive message
    function handleButtonClick(buttonId, buttonText) {
        // PASS the current timestamp when sent
        showMessage({ from: userId, body: buttonText }, new Date());

        if (stompClient && stompClient.connected) {
            // The buttonId here is either the Rasa payload or the Twilio reply ID.
            stompClient.send('/app/chat', {}, JSON.stringify({
                from: userId,
                body: buttonText,
                buttonId: buttonId
            }));
        }
    }

    // Manual message send (input + send button)
    function sendMessage() {
        const text = input.value.trim();
        if (text && stompClient && stompClient.connected) {
            // PASS the current timestamp when sent
            showMessage({ from: userId, body: text }, new Date());

            stompClient.send('/app/chat', {}, JSON.stringify({
                from: userId,
                body: text
            }));

            input.value = '';
        }
    }

    sendBtn.addEventListener('click', sendMessage);
    input.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') sendMessage();
    });

    connect();
});