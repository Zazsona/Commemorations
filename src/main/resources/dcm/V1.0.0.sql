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

CREATE TABLE IF NOT EXISTS TemplateSkinRenderDefinition (
      TemplateId       VARCHAR(255)   NOT NULL
    , SkinRenderType   TEXT CHECK( SkinRenderType IN ('HEAD', 'TORSO', 'BODY') )  NOT NULL
    , StartX           INTEGER        NOT NULL
    , StartY           INTEGER        NOT NULL
    , Width            INTEGER        NOT NULL
    , Height           INTEGER        NOT NULL
    , FOREIGN KEY(TemplateId) REFERENCES TemplateGraphic(TemplateId)
);

CREATE TABLE IF NOT EXISTS RenderedGraphic (
      RenderGuid         VARCHAR(36)    NOT NULL
    , TemplateId         VARCHAR(255)   NOT NULL
    , ImageBase64        VARCHAR(2048)  NOT NULL
    , LastUpdated        INTEGER        NOT NULL    DEFAULT CURRENT_TIMESTAMP
    , PRIMARY KEY(RenderGuid)
    , FOREIGN KEY(TemplateId) REFERENCES TemplateGraphic(TemplateId)
);

CREATE TABLE IF NOT EXISTS Player (
      PlayerGuid         VARCHAR(36)    NOT NULL
    , Username           VARCHAR(16)    NOT NULL
    , SkinBase64         VARCHAR(2048)  NOT NULL
    , LastUpdated        INTEGER        NOT NULL    DEFAULT CURRENT_TIMESTAMP
    , PRIMARY KEY(PlayerGuid)
);

CREATE TABLE IF NOT EXISTS BrgPlayerToRenderedGraphic (
      PlayerGuid         VARCHAR(36)    NOT NULL
    , RenderGuid         VARCHAR(36)    NOT NULL
    , OrderIndex         INTEGER        NOT NULL
    , FOREIGN KEY(PlayerGuid) REFERENCES Player(PlayerGuid)
    , FOREIGN KEY(RenderGuid) REFERENCES RenderedGraphic(RenderGuid)
);

-- ============================
-- Update Meta Table
-- ============================

INSERT INTO PluginMeta (MajorVersion, MinorVersion, PatchVersion)
VALUES (1, 0, 0);