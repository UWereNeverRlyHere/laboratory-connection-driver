package ywh.commons.data;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SerialPortMetaData {
    private String name;
    private boolean isBusy;
    private boolean isSelected;

    public SerialPortMetaData(String name, boolean isBusy,boolean isSelected) {
        this.name = name;
        this.isBusy = isBusy;
        this.isSelected = isSelected;
    }

    public String displayName() {
        return isBusy ? name + " (зайнятий)" : name;
    }

    @Override
    public String toString() {
        return displayName();
    }

}
