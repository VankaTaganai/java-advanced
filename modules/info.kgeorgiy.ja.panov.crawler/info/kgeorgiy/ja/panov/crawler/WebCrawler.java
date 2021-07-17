package info.kgeorgiy.ja.panov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class WebCrawler implements AdvancedCrawler {
    private static final int AWAIT_TIME = 800;
    public static final int DEFAULT_DEPTH = 1;
    public static final int DEFAULT_DOWNLOADERS_CNT = 3;
    public static final int DEFAULT_EXTRACTORS_CNT = 3;
    public static final int DEFAULT_PER_HOST = 2;

    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;
    private final Map<String, IOException> errors;
    private final Map<String, Semaphore> semaphoreByHost;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        this.errors = new ConcurrentHashMap<>();
        this.semaphoreByHost = new ConcurrentHashMap<>();
    }

    private void download(String url, boolean isLastLayer, Phaser phaser, Queue<String> downloadedURLs, Set<String> hostSet) {
        try {
            String host = URLUtils.getHost(url);

            Semaphore semaphore = semaphoreByHost.computeIfAbsent(host, q -> new Semaphore(perHost));
            phaser.register();
            downloaders.submit(() -> {
                try {
                    semaphore.acquireUninterruptibly();
                    Document document = downloader.download(url);
                    if (!isLastLayer) {
                        extract(url, document, phaser, downloadedURLs, hostSet);
                    }
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arriveAndDeregister();
                    semaphore.release();
                }
            });
        } catch (MalformedURLException e) {
            errors.put(url, e);
        }
    }

    private void extract(String url, Document document, Phaser phaser, Queue<String> downloadedURLs, Set<String> hostSet) {
        phaser.register();
        extractors.submit(() -> {
            try {
                final List<String> extractedLinks = document.extractLinks();
                downloadedURLs.addAll(hostSet == null
                        ? extractedLinks
                        : extractedLinks.stream()
                            .filter(link -> {
                                try {
                                    return hostSet.contains(URLUtils.getHost(link));
                                } catch (MalformedURLException ignore) {
                                    return false;
                                }
                            })
                            .collect(Collectors.toList()));
            } catch (IOException e) {
                errors.put(url, e);
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    @Override
    public Result download(String url, int depth) {
        return download(url, depth, null);
    }

    @Override
    public Result download(String url, int depth, List<String> hosts) {
        final Set<String> downloadedURLs = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final Set<String> hostSet = hosts == null ? null : new HashSet<>(hosts);

        final List<String> currentLayer = new ArrayList<>();
        final Queue<String> nextLayer = new ConcurrentLinkedQueue<>();
        currentLayer.add(url);

        final Phaser phaser = new Phaser(1);

        final String host;
        try {
            host = URLUtils.getHost(url);
        } catch (MalformedURLException e) {
            errors.put(url, e);
            return new Result(List.of(), errors);
        }
        if (hostSet != null && !hostSet.contains(host)) {
            return new Result(List.of(), errors);
        }

        for (int i = 1; !currentLayer.isEmpty() && i <= depth; i++) {
            final int layerInd = i;
            nextLayer.clear();
            currentLayer.stream()
                    .filter(downloadedURLs::add)
                    .forEach(curUrl -> download(curUrl, layerInd == depth, phaser, nextLayer, hostSet));
            phaser.arriveAndAwaitAdvance();
            currentLayer.clear();
            currentLayer.addAll(nextLayer);
        }
        downloadedURLs.removeAll(errors.keySet());
        return new Result(new ArrayList<>(downloadedURLs), errors);
    }

    @Override
    public void close() {
        shutDownPool(downloaders);
        shutDownPool(extractors);
    }

    private void shutDownPool(final ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(AWAIT_TIME, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public static void main(final String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments should be not null");
        } else if (args.length < 1 || args.length > 5) {
            System.err.println("Wrong arguments number: WebCrawler url [depth [downloads [extractors [perHost]]]]");
        } else {
            final int depth = getOrDefault(args, 1, DEFAULT_DEPTH);
            final int downloaders = getOrDefault(args, 2, DEFAULT_DOWNLOADERS_CNT);
            final int extractors = getOrDefault(args, 3, DEFAULT_EXTRACTORS_CNT);
            final int perHost = getOrDefault(args, 4, DEFAULT_PER_HOST);
            final String url = args[0];

            try (final WebCrawler webCrawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
                final Result result = webCrawler.download(url, depth);

                System.out.println("Downloaded URLs:");
                result.getDownloaded().forEach(System.out::println);
                System.out.println("Downloading errors:");
                result.getErrors().forEach(
                        (URL, error) -> System.out.println("URL:=[" + URL + "] " + "Error=[" + error.getMessage() + "]")
                );
            } catch (IOException e) {
                System.err.println("Downloader cannot be created=[" + e.getMessage() + "]");
            }
        }
    }

    private static int getOrDefault(final String[] args, final int ind, final int defaultArg) {
        return args.length <= ind ? defaultArg : Integer.parseInt(args[ind]);
    }
}
