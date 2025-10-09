package ywh.repository.analysis.repos;

public class RepositoryProvider {
    private RepositoryProvider() {
    }

    private static IndicatorRepository indicatorRepository;
    private static IndicatorOrderRepository indicatorOrderRepository;

    public static synchronized void initialize(IndicatorRepository indicatorRepo, IndicatorOrderRepository orderRepo) {
        RepositoryProvider.indicatorRepository = indicatorRepo;
        RepositoryProvider.indicatorOrderRepository = orderRepo;
    }

    public static IndicatorRepository indicators() {
        if (indicatorRepository == null) {
            throw new IllegalStateException("IndicatorRepository не ініціалізовано");
        }
        return indicatorRepository;
    }

    public static IndicatorOrderRepository indicatorOrder() {
        if (indicatorOrderRepository == null) {
            throw new IllegalStateException("IndicatorOrderRepository не ініціалізовано");
        }
        return indicatorOrderRepository;
    }
}
