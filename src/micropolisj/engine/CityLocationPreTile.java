package micropolisj.engine;

public class CityLocationPreTile extends CityLocation{

    private char preTile;

    public CityLocationPreTile(int x, int y, char preTile) {
        super(x, y);
        this.preTile = preTile;

    }

    public char getPreTile() {
        return preTile;
    }
}