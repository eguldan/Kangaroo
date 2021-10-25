import { useState } from 'react';

export default function Challenge() {
  // useState to define custom state variable and update function since not in a class
  const [customState, setCustomState] = useState({"totalCost": "", "lowStock": []})

  function getLowStockItems() {
    let request = new XMLHttpRequest();
    request.open("GET", "http://localhost:4567/low-stock");
    request.send();
    request.onload = () => {
      if (request.status === 200) {
        setCustomState({"totalCost": "", "lowStock": JSON.parse(request.response)})
      } else {
        console.log(`error ${request.status} ${request.statusText}`)
      }
    }
  }

  function determineReorderCost() {
    let request = new XMLHttpRequest();
    request.open("POST", "http://localhost:4567/restock-cost");
    let toSend = [];
    for (let i in customState.lowStock) {
      let item = customState.lowStock[i];
      if (("quantity" in item) && (item.quantity > 0)) {
        toSend.push({"id": item.id, "quantity": item.quantity});
      }
    }
    request.send(JSON.stringify(toSend));
    request.onload = () => {
      if (request.status === 200) {
        setCustomState({"totalCost": JSON.parse(request.response), "lowStock": customState.lowStock})
      } else {
        console.log(`error ${request.status} ${request.statusText}`)
      }
    }
  }

  function ItemRow(props) {
    // SKU, name, stock, capacity, order
    const i = props.index;

    function enterQuantity(event) {
      customState.lowStock[i]["quantity"] = event.target.value;
    }

    return (<tr>
      <td>{customState.lowStock[i].id}</td>
      <td>{customState.lowStock[i].name}</td>
      <td>{customState.lowStock[i].stock}</td>
      <td>{customState.lowStock[i].capacity}</td>
      <td><input type="text" value={customState.lowStock[i].quantity === 0 ? "" : customState.lowStock[i].quantity} onChange={enterQuantity}/></td>
    </tr>);
  }

  return (
    <>
      <table>
        <thead>
          <tr>
            <td>SKU</td>
            <td>Item Name</td>
            <td>Amount in Stock</td>
            <td>Capacity</td>
            <td>Order Amount</td>
          </tr>
        </thead>
        <tbody>
          {
            customState.lowStock.map(function(item, i) {
              return <ItemRow index={i}/>
            })
          }

        </tbody>
      </table>
      <div>Total Cost: {customState.totalCost}</div>
      <button onClick={getLowStockItems}>Get Low-Stock Items</button>
      <button onClick={determineReorderCost}>Determine Re-Order Cost</button>
    </>
  );
}
