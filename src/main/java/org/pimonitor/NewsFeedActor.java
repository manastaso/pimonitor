package org.pimonitor;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import okhttp3.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NewsFeedActor extends AbstractActorWithTimers {

    private static ActorRef ui;
    private static final Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final OkHttpClient client;

    public NewsFeedActor(ActorRef ui) {

        getSelf().tell("tick", getSelf());
        getTimers().startTimerAtFixedRate(TICK_KEY, "tick", Duration.ofMinutes(30));
        NewsFeedActor.ui = ui;
        client = new OkHttpClient();
    }

    private static final SupervisorStrategy strategy =
            new OneForOneStrategy(
                    10,
                    Duration.ofMinutes(1),
                    DeciderBuilder.match(Exception.class, e -> SupervisorStrategy.restart())
                            .matchAny(o -> SupervisorStrategy.escalate())
                            .build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public Receive createReceive() {
        return receiveBuilder().match(
                        String.class,
                        message -> ui.tell(getNews(), getSelf()))
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private List<String> getNews() throws IOException, ParserConfigurationException, SAXException {
        List<String> news = new ArrayList<>();

        HttpUrl.Builder urlBuilder
                = Objects.requireNonNull(HttpUrl.parse("http://www.ndr.de/nachrichten/hamburg/index-rss.xml")).newBuilder();

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();

        InputStream responseStream = Objects.requireNonNull(response.body()).byteStream();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(responseStream);
        doc.getDocumentElement().normalize();

        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            news.add(doc.getElementsByTagName("item").item(i).getChildNodes().item(1).getTextContent());
        }

        return news;
    }
}
