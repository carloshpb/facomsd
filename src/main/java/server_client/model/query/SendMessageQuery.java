package server_client.model.query;

import io.atomix.copycat.Query;
import server_client.model.Message;

public class SendMessageQuery implements Query<Message> {

    private Message message;

    public SendMessageQuery(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}