'use strict';

const React = require('react')
const client = require('./client')
const when = require('when')

class App extends React.Component {

	constructor(props) {
		super(props);
		this.state = {keys: [], selected: undefined}
		this.selectKey = this.selectKey.bind(this)
	}

	componentDidMount() {
		client({method: 'GET', path: '/keys'}).done(response => {
			this.setState({keys: response})
		})
	}

	selectKey(tag) {
		client({method: 'GET', path: this.state.keys.entity._links[tag].href}).done(response => {
			console.log(response.raw.response);
			this.setState({keys: this.state.keys, selected: response.raw.response});
		})
	}

	render() {
		return (
			<div className="layout">
				<KeyList keys={this.state.keys} selectKey={this.selectKey} />
				<KeyDetails details={this.state.selected} />
			</div>
		)
	}

}

class KeyList extends React.Component {

	render() {
		console.log(this.props.keys)
		var links
		if (this.props.keys.entity !== undefined) {
			links = Object.keys(this.props.keys.entity._links).map(key =>
				<KeyEntry key={this.props.keys.entity._links[key].href}
						  href={this.props.keys.entity._links[key].href}
						  tag={key}
						  selectKey={this.props.selectKey}
				/>
			)
		} else {
			links = []
		}
		return (
			<ul className="layout__item u-1\/2-lap-and-up">
				{links}
			</ul>
		)
	}

}

class KeyEntry extends React.Component {

	constructor(props) {
		super(props);
		this.handleSubmit = this.handleSubmit.bind(this);

	}

	handleSubmit(e) {
		e.preventDefault();
		this.props.selectKey(this.props.tag);
	}

	render() {
		return (
			<li>
				{this.props.tag}
				<button onClick={this.handleSubmit}>Details</button>
			</li>
		)
	}

}

class KeyDetails extends React.Component {

	render() {
		return (
			<textarea name="details" value={this.props.details} className="layout__item u-1\/2-lap-and-up"/>
		)
	}
}

React.render(
	<App/>,
	document.getElementById('react')
)