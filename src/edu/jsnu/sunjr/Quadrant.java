package edu.jsnu.sunjr;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author sunjr
 * @Date 2023/8/12/0012 22:07
 * @Version 1.0
 */

@Getter
@Setter
public class Quadrant {
    List<Integer> area = new ArrayList<>();

    public void add(int value){
        area.add(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof Quadrant) {
            Quadrant quadrant = (Quadrant) obj;
            for (int d = 0; d < area.size(); d++) {
                if (area.get(d) != quadrant.getArea().get(d)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + area.hashCode();
        return result;
    }
}
