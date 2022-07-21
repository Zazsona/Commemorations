-- ============================
-- Create Meta Table
-- ============================

CREATE TABLE IF NOT EXISTS PluginMeta (
      MajorVersion       INTEGER   NOT NULL
    , MinorVersion       INTEGER   NOT NULL
    , PatchVersion       INTEGER   NOT NULL
    , InstalledOn        INTEGER   NOT NULL   DEFAULT CURRENT_TIMESTAMP
);

-- ============================
-- Create Graphic Tables
-- ============================

CREATE TABLE IF NOT EXISTS TemplateGraphic (
      TemplateId         VARCHAR(255)   NOT NULL
    , ImageBase64        VARCHAR(2048)  NOT NULL
    , LastUpdated        INTEGER        NOT NULL    DEFAULT CURRENT_TIMESTAMP
    , PRIMARY KEY(TemplateId)
);

CREATE TABLE IF NOT EXISTS TemplateGraphicPlayerSkinMapping (
      TemplateId      VARCHAR(255)   NOT NULL
    , SkinType        TEXT CHECK( SkinType IN ('Head', 'Torso', 'Body') )  NOT NULL
    , StartX          INTEGER        NOT NULL
    , StartY          INTEGER        NOT NULL
    , Width           INTEGER        NOT NULL
    , Length          INTEGER        NOT NULL
    , FOREIGN KEY(TemplateId) REFERENCES TemplateGraphic(TemplateId)
);

CREATE TABLE IF NOT EXISTS RenderedGraphic (
      RenderId           VARCHAR(36)    NOT NULL
    , TemplateId         VARCHAR(255)   NOT NULL
    , ImageBase64        VARCHAR(2048)  NOT NULL
    , LastUpdated        INTEGER        NOT NULL    DEFAULT CURRENT_TIMESTAMP
    , PRIMARY KEY(RenderId)
    , FOREIGN KEY(TemplateId) REFERENCES TemplateGraphic(TemplateId)
);

-- ============================
-- Update Meta Table
-- ============================

INSERT INTO PluginMeta (MajorVersion, MinorVersion, PatchVersion)
VALUES (1, 0, 0);