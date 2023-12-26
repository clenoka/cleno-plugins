//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.unethicalite.plugins.clenofisher;

public enum Region
{
    USA(0),
    UK(1),
    AUS(3),
    GER(7);

    private final int code;

    private Region(int code)
    {
        this.code = code;
    }

    public int getCode()
    {
        return this.code;
    }
}
