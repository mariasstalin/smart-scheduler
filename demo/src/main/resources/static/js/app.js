document.addEventListener('DOMContentLoaded', function () {
    const messagesList = document.getElementById('messages');
    const input = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');

    let stompClient = null;

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
                showMessage(payload);
            });
        }, function (error) {
            console.error('Connection error:', error);
            sendBtn.disabled = true;
            setTimeout(connect, 5000);
        });
    }

    // Renders both normal and interactive messages
    function showMessage(payload) {
        const li = document.createElement('li');
        // FIX: Classify based on whether the sender ID matches the current user ID (userId).
        // If the sender is NOT the current user, it must be the system/bot.
        const fromType = payload.from === userId ? 'from-user' : 'from-system';
        li.className = 'message ' + fromType;

        // Avatar (using emojis instead of external images for reliability)
        const avatar = document.createElement('div');
        avatar.className = 'avatar';
        // Ensure consistent check for avatar display
        avatar.textContent = fromType === 'from-system' ? '🤖' : '👤';

        // Message bubble
        const bubble = document.createElement('div');
        bubble.className = 'bubble';

        // If message is interactive (Twilio button message simulation)
        if (payload.type === 'interactive' && payload.interactive && payload.interactive.action) {
            const bodyText = payload.interactive.body?.text || 'Select an option:';
            bubble.textContent = bodyText;

            const buttonContainer = document.createElement('div');
            buttonContainer.className = 'interactive-buttons';

            const buttons = payload.interactive.action.buttons || [];
            buttons.forEach(btn => {
                const buttonEl = document.createElement('button');
                buttonEl.className = 'interactive-button';

                // --- FIX: Correctly extract ID and Title from the nested 'reply' object ---
                const buttonId = btn.reply?.id || btn.id; // Use reply.id, fallback to id
                const buttonTitle = btn.reply?.title || btn.title || btn.text || 'Button'; // Use reply.title

                buttonEl.textContent = buttonTitle;
                buttonEl.addEventListener('click', () => handleButtonClick(buttonId, buttonTitle));
                buttonContainer.appendChild(buttonEl);
            });

            bubble.appendChild(buttonContainer);
        } else {
            // Normal message
            bubble.textContent = payload.body || payload.message || '';
        }

        li.appendChild(avatar);
        li.appendChild(bubble);
        messagesList.appendChild(li);
        messagesList.scrollTop = messagesList.scrollHeight;
    }

    // When user clicks a button from an interactive message
    function handleButtonClick(buttonId, buttonText) {
        showMessage({ from: userId, body: buttonText });

        if (stompClient && stompClient.connected) {
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
            showMessage({ from: userId, body: text });

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
