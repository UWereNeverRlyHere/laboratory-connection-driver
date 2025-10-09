package ywh.services.data.models.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ImageResult {
    private String name;
    private String image;

    public ImageResult(String name, String image) {
        this.name = name;
        this.image = image;
    }
}
