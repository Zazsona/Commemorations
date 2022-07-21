package com.zazsona.commemorations;

import java.util.Objects;

public class SemanticVersion implements Comparable<SemanticVersion>
{
    private int major;
    private int minor;
    private int patch;

    public SemanticVersion(int major, int minor, int patch)
    {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public SemanticVersion(String version)
    {
        String[] sections = version.split("[.]");
        this.major = Integer.parseInt(sections[0]);
        this.minor = Integer.parseInt(sections[1]);
        this.patch = Integer.parseInt(sections[2]);
    }

    public int getMajorVersion()
    {
        return major;
    }

    public int getMinorVersion()
    {
        return minor;
    }

    public int getPatchVersion()
    {
        return patch;
    }

    @Override
    public String toString()
    {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemanticVersion that = (SemanticVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public int compareTo(SemanticVersion o)
    {
        if (this == o) return 0;
        if (o == null || getClass() != o.getClass()) return 1;
        return this.toString().compareTo(o.toString());
    }
}
