document.addEventListener('DOMContentLoaded', function() {
    const messagesList = document.getElementById('messages');
    const input = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');

    let stompClient = null;

    function connect() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function(frame) {
            console.log('Connected: ' + frame);
            sendBtn.disabled = false;

            // Subscribe to this user's private topic
            stompClient.subscribe('/topic/messages-' + userId, function(message) {
                const payload = JSON.parse(message.body);
                showMessage(payload.body, 'from-system');
            });
        }, function(error) {
            console.error('Connection error:', error);
            sendBtn.disabled = true;
            setTimeout(connect, 5000);
        });
    }

    function showMessage(text, fromType) {
        const li = document.createElement('li');
        li.className = 'message ' + fromType;

        const avatar = document.createElement('img');
        avatar.className = 'avatar';
        avatar.src = fromType === 'from-system' ? '/images/bot-avatar.png' : '/images/user-avatar.png';

        const bubble = document.createElement('div');
        bubble.className = 'bubble';
        bubble.textContent = text;

        li.appendChild(avatar);
        li.appendChild(bubble);
        messagesList.appendChild(li);
        messagesList.scrollTop = messagesList.scrollHeight;
    }

    function sendMessage() {
        const text = input.value.trim();
        if (text && stompClient && stompClient.connected) {
            showMessage(text, 'from-user');

            stompClient.send('/app/send', {}, JSON.stringify({
                from: userId,
                body: text
            }));

            input.value = '';
        }
    }

    sendBtn.addEventListener('click', sendMessage);
    input.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') sendMessage();
    });

    connect();
});
