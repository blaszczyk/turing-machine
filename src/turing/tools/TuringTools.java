package turing.tools;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.controller.ModelController;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import turing.model.Direction;
import turing.model.State;
import turing.model.Status;
import turing.model.Step;
import turing.model.TapeCell;
import turing.model.TuringMachine;
import turing.model.Value;

public class TuringTools
{
	private static final Logger LOGGER = LogManager.getLogger(TuringTools.class);
	
	static final int SHOW_CELLS_RADIUS = 5;

	public static void step(final ModelController controller, final TuringMachine machine) throws RoseException
	{
		final Status status = machine.getStatus();
		if(!status.isRunning())
			return;
		final State state = status.getCurrentState();
		final TapeCell cell = status.getCurrentCell();
		final Value value = cell.getValue();
		for(final Step step : state.getStepTos())
			if(step.getReadValue().equals(value))
			{
				final Value writeValue = step.getWriteValue();
				cell.setValue(writeValue);
				
				final boolean directionRight = step.getDirection().equals(Direction.RIGHT);
				TapeCell nextCell = directionRight ? cell.getNext() : cell.getPrevious();
				if(nextCell == null)
				{
					nextCell = controller.createNew(TapeCell.class);
					if(directionRight)
					{
						cell.setNext(nextCell);
						nextCell.setPrevious(cell);
					}
					else
					{
						cell.setPrevious(nextCell);
						nextCell.setNext(cell);
					}
				}
				status.setCurrentCell(nextCell);
				cell.setStatus(null);
				nextCell.setStatus(status);

				final State nextState = step.getStateTo();
				if(!EntityUtils.equals(state, nextState))
				{
					status.setCurrentState(nextState);
					state.getStatuss().remove(status);
					nextState.getStatuss().add(status);
					controller.update(state, nextState);
				}
				
				controller.update(status, cell, nextCell);
				
				LOGGER.info(String.format("step: machine %s with program %s", machine.getName(), machine.getProgram().getName()));
				LOGGER.info(String.format("step: from state %s to state %s.", state.getName(), nextState.getName()));
				LOGGER.info("step: tape direction = " + step.getDirection());
				LOGGER.info("Tape: " + tapeToString(SHOW_CELLS_RADIUS, status.getCurrentCell(), "[%s]"));
				return;
			}
		status.setRunning(false);
		controller.update(status);
		LOGGER.info(String.format("terminating %s with program %s at state %s.", machine.getName(), machine.getProgram().getName(), state.getName()));
		LOGGER.info("Tape: " + tapeToString(SHOW_CELLS_RADIUS, status.getCurrentCell(), "[%s]"));
	}
	
	static String tapeToString(final int radius, final TapeCell currentCell, final String highlighter)
	{
		TapeCell showCell = currentCell;
		final StringBuilder builder = new StringBuilder();
		for(int i = 0; i < radius; i++)
		{
			if(showCell.getPrevious() == null)
				break;
			showCell = showCell.getPrevious();
		}
		if(showCell.getPrevious() != null)
			builder.append("... - ");
		while(showCell != currentCell)
		{
			builder.append(showCell.getValue() + " - " );
			showCell = showCell.getNext();
		}
		builder.append(String.format(highlighter, showCell.getValue()));
		for(int i = 0; i < radius; i++)
		{
			showCell = showCell.getNext();
			if(showCell == null)
				break;
			builder.append( " - " + showCell.getValue());
		}
		if(showCell != null && showCell.getNext() != null)
			builder.append(" - ...");
		return builder.toString();
	}

	public static void editTape(final ModelController controller, final TapeCell currentCell, final List<Value> values, final int pos) throws RoseException
	{
		TapeCell cell = isCyclic(currentCell) ? currentCell : getFirst(currentCell);
		int countDown = pos;
		for(final Value value : values)
		{
			cell.setValue(value);
			if(countDown == 0)
			{
				final Status status = currentCell.getStatus();
				currentCell.setStatus(null);
				cell.setStatus(status);
				status.setCurrentCell(cell);
			}
			controller.update(cell);
			countDown--;
			cell = cell.getNext();
		}
	}
	
	public static boolean isCyclic(final TapeCell currentCell)
	{
		TapeCell cell = currentCell.getPrevious();
		while(true)
		{
			if(cell == null)
				return false;
			if(cell == currentCell)
				return true;
			cell = cell.getPrevious();
		}
	}
	
	public static TapeCell getFirst(final TapeCell currentCell)
	{
		TapeCell cell = currentCell;
		while(cell.getPrevious() != null)
			cell = cell.getPrevious();
		return cell;
	}

}
