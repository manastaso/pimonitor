package org.pimonitor;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import okhttp3.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Date;
import java.util.StringTokenizer;

public class NamedayActor  extends AbstractActorWithTimers {

    private static ActorRef ui;
    private static Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final OkHttpClient client;

    public NamedayActor(ActorRef ui) {

        getSelf().tell("tick", getSelf());
        getTimers().startTimerAtFixedRate(TICK_KEY, "tick", Duration.ofMinutes(60));
        NamedayActor.ui = ui;
        client = new OkHttpClient();

    }

    public Receive createReceive() {
        return receiveBuilder().match(
                        String.class,
                        message -> {
                            ui.tell(getNameday(), getSelf());
                        })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private String getNameday() throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
        String nameday="unknown nameday";

        HttpUrl.Builder urlBuilder
                = HttpUrl.parse("https://www.eortologio.net/rss/today.xml").newBuilder();

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();

        InputStream responseStream = response.body().byteStream();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(responseStream);
        doc.getDocumentElement().normalize();

        nameday = doc.getElementsByTagName("item").item(0).getTextContent();
        if (nameday != null) {
            nameday = nameday.trim();
            StringTokenizer tokenizer = new StringTokenizer(nameday, "(");
            if (tokenizer.hasMoreTokens()) {
                nameday = tokenizer.nextToken();
                nameday = nameday.trim();
            }
        }

        nameday = nameday + " (" + new Date() + ")";

        return nameday;
    }
}
