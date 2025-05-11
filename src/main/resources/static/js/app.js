class ChatApp {
    constructor() {
        this.stompClient = null;
        this.username = null;
        this.messageHistory = [];
        this.connected = false;
        this.emojiList = ["😀", "😂", "😍", "👍", "👋", "❤️", "🎉", "🤔"];

        this.initElements();
        this.initEventListeners();
    }

    initElements() {
        this.$connectBtn = $('#connect');
        this.$disconnectBtn = $('#disconnect');
        this.$chatArea = $('#chatArea');
        this.$nameInput = $('#name');
        this.$messagesDiv = $('#messages');
        this.$messageInput = $('#message');
        this.$receiverInput = $('#receiver');
        this.$privateMessageInput = $('#privateMessage');
        this.$sendBtn = $('#send');
        this.$sendPrivateBtn = $('#sendPrivate');
        this.$onlineUsersDiv = $('#onlineUsers');
        this.$emojiPicker = $('.emoji-picker');
    }

    initEventListeners() {
        this.$connectBtn.on('click', this.connect.bind(this));
        this.$disconnectBtn.on('click', this.disconnect.bind(this));
        this.$sendBtn.on('click', this.sendMessage.bind(this));
        this.$sendPrivateBtn.on('click', this.sendPrivateMessage.bind(this));

        this.$messageInput.on('keypress', (e) => {
            if (e.which === 13) this.sendMessage();
        });

        this.$privateMessageInput.on('keypress', (e) => {
            if (e.which === 13) this.sendPrivateMessage();
        });

        this.$emojiPicker.on('click', this.showEmojiPicker.bind(this));
    }

    connect() {
        this.username = this.$nameInput.val().trim();
        if (!this.username) {
            this.showError("Имя пользователя не может быть пустым");
            return;
        }

        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, (frame) => {
            this.connected = true;
            this.updateUI();

            console.log("Connected to server, setting up subscriptions...");

            this.setupSubscriptions();
            this.notifyJoin();
        }, (error) => {
            this.showError("Ошибка подключения: " + error);
        });
    }

    disconnect() {
        if (this.stompClient !== null) {
            this.sendSystemMessage(`${this.username} покинул чат`, 'LEAVE');
            this.stompClient.disconnect();
            this.connected = false;
            this.updateUI();
            this.$messagesDiv.empty();
            this.messageHistory = [];
        }
    }

    updateUI() {
        this.$connectBtn.prop('disabled', this.connected);
        this.$disconnectBtn.prop('disabled', !this.connected);
        this.$chatArea.toggle(this.connected);
    }

    setupSubscriptions() {
        this.stompClient.subscribe('/topic/public', (message) => {
            const chatMessage = JSON.parse(message.body);
            this.displayMessage(chatMessage);
        });

        this.stompClient.subscribe('/user/queue/private', (message) => {
            const chatMessage = JSON.parse(message.body);
            this.displayPrivateMessage(chatMessage);
        });

        this.stompClient.subscribe('/user/queue/errors', (message) => {
            const error = JSON.parse(message.body);
            this.showError(error.content);
        });

        console.log("Subscriptions set up for user: " + this.username);
    }

    notifyJoin() {
        const joinMessage = {
            sender: this.username,
            content: 'присоединился к чату',
            type: 'JOIN'
        };
        this.stompClient.send("/app/chat.addUser", {}, JSON.stringify(joinMessage));
    }

    sendMessage() {
        const messageContent = this.$messageInput.val().trim();
        if (messageContent && this.username) {
            const chatMessage = {
                sender: this.username,
                content: this.escapeHtml(messageContent),
                type: 'CHAT'
            };
            this.stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
            this.$messageInput.val("");
        }
    }

    sendPrivateMessage() {
        const receiver = this.$receiverInput.val().trim();
        const messageContent = this.$privateMessageInput.val().trim();

        if (!receiver || !messageContent) {
            this.showError("Укажите получателя и сообщение");
            return;
        }

        if (this.stompClient !== null) {
            const chatMessage = {
                sender: this.username,
                receiver: receiver,
                content: this.escapeHtml(messageContent),
                type: 'PRIVATE'
            };

            this.stompClient.send("/app/chat.private", {}, JSON.stringify(chatMessage));
            this.$privateMessageInput.val("");
        }
    }

    sendSystemMessage(content, type) {
        if (this.stompClient !== null) {
            const chatMessage = {
                sender: 'SYSTEM',
                content: content,
                type: type
            };
            this.stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        }
    }

    displayMessage(message) {
        let messageClass = 'chat-message';
        switch (message.type) {
            case 'JOIN': messageClass = 'join-message'; break;
            case 'LEAVE': messageClass = 'leave-message'; break;
            case 'SYSTEM': messageClass = 'system-message'; break;
            case 'ACTION': messageClass = 'action-message'; break;
        }

        const messageElement = $(`<div class="message ${messageClass} new-message">
            <strong>${this.escapeHtml(message.sender)}:</strong>
            ${this.escapeHtml(message.content)}
        </div>`);

        this.$messagesDiv.append(messageElement);
        this.scrollToBottom();
        this.messageHistory.push(message);
        if (this.messageHistory.length > 100) this.messageHistory.shift();
    }

    displayPrivateMessage(message) {

        console.log("Received private message:", message);

        let messageElement;
        const isIncoming = message.sender !== this.username;

        if (isIncoming) {
            messageElement = $(`<div class="message incoming-private new-message">
                <strong>Приватно от ${this.escapeHtml(message.sender)}:</strong>
                ${this.escapeHtml(message.content)}
            </div>`);
        } else {
            messageElement = $(`<div class="message outgoing-private new-message">
                <strong>Вы → ${this.escapeHtml(message.receiver)}:</strong>
                ${this.escapeHtml(message.content)}
            </div>`);
        }

        this.$messagesDiv.append(messageElement);
        this.scrollToBottom();
        this.messageHistory.push(message);
        if (this.messageHistory.length > 100) this.messageHistory.shift();
    }

    showEmojiPicker(event) {
        const emojiPicker = $("<div>").css({
            position: 'absolute',
            background: 'white',
            border: '1px solid #ddd',
            padding: '5px',
            'z-index': '1000'
        });

        this.emojiList.forEach(emoji => {
            $("<span>").text(emoji).css({
                margin: '5px',
                cursor: 'pointer'
            }).on('click', () => {
                this.$messageInput.val(this.$messageInput.val() + emoji);
                emojiPicker.remove();
            }).appendTo(emojiPicker);
        });

        emojiPicker.insertAfter($(event.target));
    }

    scrollToBottom() {
        this.$messagesDiv.scrollTop(this.$messagesDiv[0].scrollHeight);
    }

    showError(message) {
        alert(message);
    }

    escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
}

$(document).ready(() => {
    window.chatApp = new ChatApp();
});