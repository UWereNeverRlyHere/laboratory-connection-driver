package ywh.fx_app.application;

import ywh.fx_app.app_exceptions.ApplicationRuntimeException;
import ywh.logging.MainLogger;
import ywh.repository.analysis.repos.RepositoryProvider;
import ywh.repository.analysis.repos.impl.JsonIndicatorOrderRepositoryImpl;
import ywh.repository.analysis.repos.impl.JsonIndicatorRepositoryImpl;
import ywh.services.data.enums.FileResultActions;
import ywh.services.device.parsers.IParser;
import ywh.services.device.parsers.ParserAbstract;
import ywh.services.device.parsers.hti.MicroCCVet;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

public class AppStaticConfig {
    public static final String APP_NAME = "Mini laboratory connection driver V 0.3.2710";
    public static final List<FileResultActions> ALLOWED_ACTIONS = List.of(
            //FileResultActions.PRINT,
            // FileResultActions.SAVE_PDF,
            //FileResultActions.SAVE_DOCX,
            //FileResultActions.API
            // FileResultActions.SEND,
            FileResultActions.CREATE_DBF_FILE
    );
    public static final List<IParser> PARSERS = List.of(
            // ParserAbstract.create("Mindray BC30Vet Primary", new MindrayBC30Vet())
            // ParserAbstract.create("Dymind DF50 Vet", new DymindDF50Vet())
            // ParserAbstract.create("Mindray BC 700", new Mindray700Series())
            // ParserAbstract.create("ISE MIURA ASTM", new MIURA())
            ParserAbstract.create("Micro CC 20 Plus VET", new MicroCCVet())
            // ParserAbstract.create("Fujifilm DriChem NX 600 Primary", new DryChemNX600Vet())
    );

    static {
        try {
            RepositoryProvider.initialize(new JsonIndicatorRepositoryImpl(Path.of("jsonRepository")), new JsonIndicatorOrderRepositoryImpl(Path.of("jsonRepository")));
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            MainLogger.error("Failed to load Repositories: " + e.getMessage(), e);
            throw new ApplicationRuntimeException("Failed to load Repositories", e);
        }
    }

    private AppStaticConfig() {
    }
}
