package ywh.services.data.models.observation;

import ywh.repository.analysis.entities.Indicator;

public record PrintIndicatorResultModel(Indicator indicator, String value, Deviation deviation) {
}

