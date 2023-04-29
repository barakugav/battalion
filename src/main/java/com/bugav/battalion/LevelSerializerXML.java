package com.bugav.battalion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.bugav.battalion.core.Building;
import com.bugav.battalion.core.Cell;
import com.bugav.battalion.core.Level;
import com.bugav.battalion.core.LevelBuilder;
import com.bugav.battalion.core.Team;
import com.bugav.battalion.core.Terrain;
import com.bugav.battalion.core.Unit;
import com.bugav.battalion.core.Level.BuildingDesc;
import com.bugav.battalion.core.Level.UnitDesc;
import com.bugav.battalion.util.Iter;

class LevelSerializerXML implements LevelSerializer {

	private boolean formatPretty = false;

	void setFormatPretty(boolean formatPretty) {
		this.formatPretty = formatPretty;
	}

	@Override
	public void levelWrite(Level level, String outpath) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document dom = builder.newDocument();
			Element levelElm = dom.createElement("level");

			Element shapeElm = dom.createElement("shape");
			shapeElm.setAttribute("width", Integer.toString(level.width()));
			shapeElm.setAttribute("height", Integer.toString(level.height()));
			levelElm.appendChild(shapeElm);

			Element teamsElm = dom.createElement("teams");
			for (Team team : Team.values()) {
				Element teamElm = dom.createElement("team");
				teamElm.setAttribute("color", teamWrite(team));
				teamElm.setAttribute("startingMoney", Integer.toString(level.getStartingMoney(team)));
				teamsElm.appendChild(teamElm);
			}
			levelElm.appendChild(teamsElm);

			Element tilesElm = dom.createElement("tiles");
			for (Iter.Int it = Cell.Iter2D.of(level.width(), level.height()); it.hasNext();) {
				int cell = it.next();

				Element tileElm = dom.createElement("tile");
				tileElm.setAttribute("x", Integer.toString(Cell.x(cell)));
				tileElm.setAttribute("y", Integer.toString(Cell.y(cell)));

				tileElm.setAttribute("terrain", level.terrain(cell).toString());

				BuildingDesc building = level.building(cell);
				if (building != null) {
					Element buildingElm = dom.createElement("building");
					buildingElm.setAttribute("type", building.type.toString());
					buildingElm.setAttribute("team", teamWrite(building.team));
					buildingElm.setAttribute("active", building.active ? "1" : "0");
					tileElm.appendChild(buildingElm);
				}

				UnitDesc unit = level.unit(cell);
				if (unit != null) {
					Element unitElm = dom.createElement("unit");
					unitElm.setAttribute("type", unit.type.toString());
					unitElm.setAttribute("team", teamWrite(unit.team));
					unitElm.setAttribute("health", Integer.toString(unit.health));
					unitElm.setAttribute("active", unit.active ? "1" : "0");
					unitElm.setAttribute("repairing", unit.repairing ? "1" : "0");
					if (unit.type.transportUnits) {
						UnitDesc transportedUnit = unit.getTransportedUnit();
						Element transportedUnitElm = dom.createElement("transportedUnit");
						transportedUnitElm.setAttribute("type", transportedUnit.type.toString());
						transportedUnitElm.setAttribute("team", teamWrite(transportedUnit.team));
						transportedUnitElm.setAttribute("health", Integer.toString(transportedUnit.health));
						if (transportedUnit.type.transportUnits)
							throw new IllegalArgumentException();
						unitElm.appendChild(transportedUnitElm);
					}
					tileElm.appendChild(unitElm);
				}

				tilesElm.appendChild(tileElm);
			}
			levelElm.appendChild(tilesElm);

			dom.appendChild(levelElm);

			Transformer tr = TransformerFactory.newInstance().newTransformer();
			tr.setOutputProperty(OutputKeys.METHOD, "xml");
			tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			if (formatPretty) {
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			}
			File outfile = new File(outpath);
			new File(outfile.getParent()).mkdirs();
			tr.transform(new DOMSource(dom), new StreamResult(new FileOutputStream(outfile)));

		} catch (ParserConfigurationException | TransformerException | FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static Iter<Node> childrenNodes(Node parent) {
		return new Iter<>() {

			final NodeList nodes = parent.getChildNodes();
			int idx = 0;

			@Override
			public boolean hasNext() {
				return idx < nodes.getLength();
			}

			@Override
			public Node next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return nodes.item(idx++);
			}
		};
	}

	private static Iter<Element> childElms(Node parent) {
		return childrenNodes(parent).filter(n -> n.getNodeType() == Node.ELEMENT_NODE).map(n -> (Element) n);
	}

	private static Element childElmMaybeNull(Node parent, String tag) {
		for (Element elm : childElms(parent).forEach())
			if (tag.equals(elm.getTagName()))
				return elm;
		return null;
	}

	private static Element childElm(Node parent, String tag) {
		Element elm = childElmMaybeNull(parent, tag);
		if (elm == null)
			throw new RuntimeException("Element not found: '" + tag + "'");
		return elm;
	}

	@Override
	public Level levelRead(String path) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder domBuilder = factory.newDocumentBuilder();
			Document dom = domBuilder.parse(path);
			Element levelElm = dom.getDocumentElement();

			Element shapeElm = childElm(levelElm, "shape");

			final int width = Integer.parseInt(shapeElm.getAttribute("width"));
			final int height = Integer.parseInt(shapeElm.getAttribute("height"));

			LevelBuilder builder = new LevelBuilder(width, height);

			for (Element teamElm : childElms(childElm(levelElm, "teams")).forEach()) {
				Team team = teamRead(teamElm.getAttribute("color"));
				int startingMoney = Integer.parseInt(teamElm.getAttribute("startingMoney"));
				builder.setStartingMoney(team, startingMoney);
			}

			for (Element tileElm : childElms(childElm(levelElm, "tiles")).forEach()) {
				int x = Integer.parseInt(tileElm.getAttribute("x"));
				int y = Integer.parseInt(tileElm.getAttribute("y"));
				Terrain terrain = Terrain.valueOf(tileElm.getAttribute("terrain"));
				builder.setTerrain(Cell.of(x, y), terrain);

				BuildingDesc building = null;
				Element buildingElm = childElmMaybeNull(tileElm, "building");
				if (buildingElm != null) {
					String type = buildingElm.getAttribute("type");
					String team = buildingElm.getAttribute("team");
					boolean active = "1".equals(buildingElm.getAttribute("active"));
					building = BuildingDesc.of(Building.Type.valueOf(type), teamRead(team), active);
				}
				builder.setBuilding(Cell.of(x, y), building);

				UnitDesc unit = null;
				Element unitElm = childElmMaybeNull(tileElm, "unit");
				if (unitElm != null) {
					Unit.Type type = Unit.Type.valueOf(unitElm.getAttribute("type"));
					Team team = teamRead(unitElm.getAttribute("team"));
					int health = Integer.parseInt(unitElm.getAttribute("health"));
					boolean active = "1".equals(unitElm.getAttribute("active"));
					boolean repairing = "1".equals(unitElm.getAttribute("repairing"));
					if (!type.transportUnits) {
						unit = UnitDesc.of(type, team, health, active, repairing);
					} else {
						Element transportedUnitElm = childElm(unitElm, "transportedUnit");
						Unit.Type transportedUnitType = Unit.Type.valueOf(transportedUnitElm.getAttribute("type"));
						Team transportedUnitTeam = teamRead(transportedUnitElm.getAttribute("team"));
						int transportedUnitHealth = Integer.parseInt(transportedUnitElm.getAttribute("health"));
						if (transportedUnitType.transportUnits)
							throw new IllegalArgumentException();
						UnitDesc transportedUnit = UnitDesc.of(transportedUnitType, transportedUnitTeam,
								transportedUnitHealth, false, false);
						if (team != transportedUnitTeam)
							throw new IllegalArgumentException();
						unit = UnitDesc.transporter(type, transportedUnit, health, active, repairing);
					}
				}
				builder.setUnit(Cell.of(x, y), unit);
			}

			return builder.buildLevel();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Team teamRead(String s) {
		return s.equals("None") ? null : Team.valueOf(s);
	}

	private static String teamWrite(Team t) {
		return t == null ? "None" : t.toString();
	}

	@Override
	public String getFileType() {
		return "xml";
	}

}
